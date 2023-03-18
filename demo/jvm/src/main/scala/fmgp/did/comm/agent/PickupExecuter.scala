package fmgp.did.comm.agent

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.Operations._
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.pickup3._

object PickupExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services & Ref[MediatorDB]] {

  override def suportedPIURI: Seq[PIURI] = Seq(
    StatusRequest.piuri,
    Status.piuri,
    DeliveryRequest.piuri,
    MessageDelivery.piuri,
    MessagesReceived.piuri,
    LiveModeChange.piuri,
  )

  override def program[R1 <: Ref[MediatorDB]](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Action] = {
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
      case m: StatusRequest => ZIO.succeed(NoReply) // FIXME
      case m: Status        => ZIO.logInfo("Status") *> ZIO.succeed(NoReply)
      case m: DeliveryRequest =>
        for {
          _ <- ZIO.logInfo("MediateGrant")
          db <- ZIO.service[Ref[MediatorDB]]
          mediatorDB <- db.get
          didRequestingMessages = m.from.asFROMTO
          messages = mediatorDB.getMessages(
            to = didRequestingMessages.toDID,
            from = m.recipient_did.map(_.toDID)
          )
          deliveryRequest = MessageDelivery(
            thid = m.id,
            from = m.to.asFROM,
            to = m.from.asTO,
            recipient_did = m.recipient_did,
            attachments = messages.map(m => (m.hashCode.toString, m)).toMap,
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
      case m: MessagesReceived => ZIO.logInfo("MessagesReceived") *> ZIO.succeed(NoReply)
      case m: LiveModeChange   => ZIO.logWarning("LiveModeChange not implemented") *> ZIO.succeed(NoReply) // TODO

    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program
  }

}
