package io.iohk.atala.mediator.protocols

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.Operations.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.pickup3.*
import io.iohk.atala.mediator.*
import io.iohk.atala.mediator.db.*
import zio.*
import zio.json.*
import fmgp.did.framework._
import fmgp.did.framework.Transport.TransmissionType

object PickupExecuter
    extends ProtocolExecuter[
      UserAccountRepo & MessageItemRepo & TransportDIDComm[Any] & Ref[MediatorTransportManager],
      MediatorError | StorageError
    ] {

  override def supportedPIURI: Seq[PIURI] = Seq(
    StatusRequest.piuri,
    Status.piuri,
    DeliveryRequest.piuri,
    MessageDelivery.piuri,
    MessagesReceived.piuri,
    LiveModeChange.piuri,
  )

  override def program(plaintextMessage: PlaintextMessage) = {
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
          mediatorTransportManager <- ZIO.service[Ref[MediatorTransportManager]].flatMap(_.get)
          transport <- ZIO.service[TransportDIDComm[Any]]
          live_delivery = mediatorTransportManager.isLiveModeEnabled(m.from.asFROMTO, transport.id)
          ret = mDidAccount match
            case None =>
              Problems
                .notEnroledError(
                  from = m.to.asFROM,
                  to = Some(m.from.asTO),
                  pthid = m.id, // TODO CHECK pthid
                  piuri = m.piuri,
                  didNotEnrolled = didRequestingMessages.asFROM.toDIDSubject,
                )
                .toPlaintextMessage
            case Some(didAccount) =>
              val msgHash = didAccount.messagesRef.filter(_.state == false).map(_.hash)
              Status(
                thid = m.id,
                from = m.to.asFROM,
                to = m.from.asTO,
                recipient_did = m.recipient_did,
                message_count = msgHash.size,
                longest_waited_seconds = None, // TODO
                newest_received_time = None, // TODO
                oldest_received_time = None, // TODO
                total_bytes = None, // TODO
                live_delivery = Some(live_delivery),
              ).toPlaintextMessage
        } yield Reply(ret)
      case m: Status =>
        ZIO.logInfo("Status") *>
          ZIO.succeed(
            Reply(
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
          ret <- mDidAccount match
            case None =>
              ZIO.succeed(
                Problems
                  .notEnroledError(
                    from = m.to.asFROM,
                    to = Some(m.from.asTO),
                    pthid = m.id, // TODO CHECK pthid
                    piuri = m.piuri,
                    didNotEnrolled = didRequestingMessages.asFROM.toDIDSubject,
                  )
                  .toPlaintextMessage
              )
            case Some(didAccount) =>
              val msgHash = didAccount.messagesRef.filter(_.state == false).map(_.hash)
              if (msgHash.isEmpty) {
                for {
                  mediatorTransportManager <- ZIO.service[Ref[MediatorTransportManager]].flatMap(_.get)
                  transport <- ZIO.service[TransportDIDComm[Any]]
                  live_delivery = mediatorTransportManager.isLiveModeEnabled(m.from.asFROMTO, transport.id)
                  ret = Status(
                    thid = m.id,
                    from = m.to.asFROM,
                    to = m.from.asTO,
                    recipient_did = m.recipient_did,
                    message_count = msgHash.size,
                    longest_waited_seconds = None, // TODO
                    newest_received_time = None, // TODO
                    oldest_received_time = None, // TODO
                    total_bytes = None, // TODO
                    live_delivery = Some(live_delivery)
                  ).toPlaintextMessage
                } yield ret
              } else {
                for {
                  allMessagesFor <- repoMessageItem.findByIds(msgHash)
                  messagesToReturn =
                    if (m.recipient_did.isEmpty) allMessagesFor
                    else {
                      allMessagesFor.filterNot(item =>
                        item.msg match {
                          case sMsg: SignedMessage =>
                            sMsg.payloadAsPlaintextMessage
                              .map(_.to.toSeq.flatMap(i => i))
                              .getOrElse(Seq.empty)
                              .map(_.toDID) // All Recipient Of The Message
                              .forall(e => !m.recipient_did.map(_.toDID.did).contains(e))
                          case eMsg: EncryptedMessage =>
                            eMsg.recipientsSubject
                              .map(_.did)
                              .forall(e => !m.recipient_did.map(_.toDID.did).contains(e))
                        }
                      )
                    }
                } yield MessageDelivery(
                  thid = m.id,
                  from = m.to.asFROM,
                  to = m.from.asTO,
                  recipient_did = m.recipient_did,
                  attachments = messagesToReturn.map(m => (m._id, m.msg)).toMap,
                ).toPlaintextMessage
              }
        } yield Reply(ret)
      case m: MessageDelivery =>
        ZIO.logInfo("MessageDelivery") *>
          ZIO.succeed(
            Reply(
              MessagesReceived(
                thid = Some(m.id),
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
        for {
          _ <- ZIO.logInfo("LiveModeChange")
          // For Live Mode
          refMediatorTransportManager <- ZIO.service[Ref[MediatorTransportManager]]
          transport <- ZIO.service[TransportDIDComm[Any]]
          _ <- ZIO.log(s"The transport's transmissionType is of the type ${transport.transmissionType}")
          ret <-
            transport.transmissionType match // If sent with live_delivery set to true on a connection incapable of live delivery, a problem_report SHOULD be sent
              case TransmissionType.SingleTransmission => // Like HTTP
                ZIO
                  .log(s"Connection '${transport.id}' does not support Live Delivery")
                  .map(_ =>
                    Problems
                      .liveModeNotSupported(
                        from = m.to.asFROM,
                        to = m.from.asTO,
                        pthid = m.id,
                        piuri = m.piuri,
                      )
                      .toPlaintextMessage
                  )
              case TransmissionType.MultiTransmissions => // Like WS
                for {
                  updateTask <- refMediatorTransportManager.update(tm =>
                    if (m.live_delivery) tm.enableLiveMode(m.from.asFROMTO, transport.id)
                    else tm.disableLiveMode(m.from.asFROMTO, transport.id)
                  )
                  // Make the status reply
                  repoDidAccount <- ZIO.service[UserAccountRepo]
                  didRequestingMessages = m.from.asFROMTO
                  mDidAccount <- repoDidAccount.getDidAccount(didRequestingMessages.toDID)
                  ret = mDidAccount match
                    case None =>
                      Problems
                        .notEnroledError(
                          from = m.to.asFROM,
                          to = Some(m.from.asTO),
                          pthid = m.id, // TODO CHECK pthid
                          piuri = m.piuri,
                          didNotEnrolled = didRequestingMessages.asFROM.toDIDSubject,
                        )
                        .toPlaintextMessage
                    case Some(didAccount) =>
                      val msgHash = didAccount.messagesRef.filter(_.state == false).map(_.hash)
                      Status(
                        thid = m.id,
                        from = m.to.asFROM,
                        to = m.from.asTO,
                        recipient_did = None, // m.recipient_did,
                        message_count = msgHash.size,
                        longest_waited_seconds = None, // TODO
                        newest_received_time = None, // TODO
                        oldest_received_time = None, // TODO
                        total_bytes = None, // TODO
                        live_delivery = Some(m.live_delivery),
                      ).toPlaintextMessage
                } yield ret
        } yield Reply(ret)
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program
  }

}
