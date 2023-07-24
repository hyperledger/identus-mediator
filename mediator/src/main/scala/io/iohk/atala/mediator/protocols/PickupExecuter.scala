package io.iohk.atala.mediator.protocols

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.Operations.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.pickup3.*
import io.iohk.atala.mediator.*
import io.iohk.atala.mediator.actions.*
import io.iohk.atala.mediator.db.*
import zio.*
import zio.json.*
object PickupExecuter
    extends ProtocolExecuterWithServices[ProtocolExecuter.Services & UserAccountRepo & MessageItemRepo] {

  override def suportedPIURI: Seq[PIURI] = Seq(
    StatusRequest.piuri,
    Status.piuri,
    DeliveryRequest.piuri,
    MessageDelivery.piuri,
    MessagesReceived.piuri,
    LiveModeChange.piuri,
  )

  override def program[R1 <: UserAccountRepo & MessageItemRepo](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, MediatorError, Action] = {
    // the val is from the match to be definitely stable
    val piuriStatusRequest = StatusRequest.piuri
    val piuriStatus = Status.piuri
    val piuriDeliveryRequest = DeliveryRequest.piuri
    val piuriMessageDelivery = MessageDelivery.piuri
    val piuriMessagesReceived = MessagesReceived.piuri
    val piuriLiveModeChange = LiveModeChange.piuri

    (plaintextMessage.`type` match {
      case `piuriStatusRequest`    => plaintextMessage.toStatusRequest
      case `piuriStatus`           => plaintextMessage.toStatus
      case `piuriDeliveryRequest`  => plaintextMessage.toDeliveryRequest
      case `piuriMessageDelivery`  => plaintextMessage.toMessageDelivery
      case `piuriMessagesReceived` => plaintextMessage.toMessagesReceived
      case `piuriLiveModeChange`   => plaintextMessage.toLiveModeChange
    }).map {
      case m: StatusRequest =>
        for {
          _ <- ZIO.logInfo("StatusRequest")
          repoDidAccount <- ZIO.service[UserAccountRepo]
          didRequestingMessages = m.from.asFROMTO
          mDidAccount <- repoDidAccount.getDidAccount(didRequestingMessages.toDID)
          msgHash = mDidAccount match
            case None             => ??? // TODO FIXME
            case Some(didAccount) => didAccount.messagesRef.filter(_.state == false).map(_.hash)
          status = Status(
            thid = m.id,
            from = m.to.asFROM,
            to = m.from.asTO,
            recipient_did = m.recipient_did,
            message_count = msgHash.size,
            longest_waited_seconds = None, // TODO
            newest_received_time = None, // TODO
            oldest_received_time = None, // TODO
            total_bytes = None, // TODO
            live_delivery = None, // TODO
          )
        } yield SyncReplyOnly(status.toPlaintextMessage)
      case m: Status =>
        ZIO.logInfo("Status") *>
          ZIO.succeed(
            SyncReplyOnly(
              Problems
                .unsupportedProtocolRole(
                  from = m.to.asFROM,
                  to = m.from.asTO,
                  pthid = m.id, // TODO CHECK pthid
                  piuri = m.piuri,
                )
                .toPlaintextMessage
            )
          )
      case m: DeliveryRequest =>
        for {
          _ <- ZIO.logInfo("DeliveryRequest")
          repoMessageItem <- ZIO.service[MessageItemRepo]
          repoDidAccount <- ZIO.service[UserAccountRepo]
          didRequestingMessages = m.from.asFROMTO
          mDidAccount <- repoDidAccount.getDidAccount(didRequestingMessages.toDID)
          msgHash = mDidAccount match
            case None             => ??? // TODO ERROR
            case Some(didAccount) => didAccount.messagesRef.filter(_.state == false).map(_.hash)
          allMessagesFor <- repoMessageItem.findByIds(msgHash)
          messagesToReturn =
            if (m.recipient_did.isEmpty) allMessagesFor
            else {
              allMessagesFor.filterNot(
                _.msg.recipientsSubject
                  .map(_.did)
                  .forall(e => !m.recipient_did.map(_.toDID.did).contains(e))
              )
            }
          deliveryRequest = MessageDelivery(
            thid = m.id,
            from = m.to.asFROM,
            to = m.from.asTO,
            recipient_did = m.recipient_did,
            attachments = messagesToReturn.map(m => (m._id, m.msg)).toMap,
          )
        } yield SyncReplyOnly(deliveryRequest.toPlaintextMessage)
      case m: MessageDelivery =>
        ZIO.logInfo("MessageDelivery") *>
          ZIO.succeed(
            Reply(
              MessagesReceived(
                thid = m.id,
                from = m.to.asFROM,
                to = m.from.asTO,
                message_id_list = m.attachments.keys.toSeq,
              ).toPlaintextMessage
            )
          )
      case m: MessagesReceived =>
        for {
          _ <- ZIO.logInfo("MessagesReceived")
          repoDidAccount <- ZIO.service[UserAccountRepo]
          didRequestingMessages = m.from.asFROMTO
          mDidAccount <- repoDidAccount.markAsDelivered(
            didRequestingMessages.toDID,
            m.message_id_list
          )
        } yield NoReply
      case m: LiveModeChange =>
        ZIO.logWarning("LiveModeChange not implemented") *>
          ZIO.succeed(
            SyncReplyOnly(
              Problems
                .protocolNotImplemented(
                  from = m.to.asFROM,
                  to = m.from.asTO,
                  pthid = m.id, // TODO CHECK pthid
                  piuri = m.piuri,
                )
                .toPlaintextMessage
            )
          )

    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program
  }

}
