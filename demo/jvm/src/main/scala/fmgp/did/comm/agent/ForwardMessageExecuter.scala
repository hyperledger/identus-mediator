package fmgp.did.comm.agent

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.routing2._

object ForwardMessageExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services & Ref[MediatorDB]] {

  override def suportedPIURI: Seq[PIURI] = Seq(ForwardMessage.piuri)

  override def program[R1 <: Ref[MediatorDB]](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Option[PlaintextMessage]] = {
    // the val is from the match to be definitely stable
    val piuriForwardMessage = ForwardMessage.piuri

    (plaintextMessage.`type` match {
      case `piuriForwardMessage` => plaintextMessage.toForwardMessage
    }).map { case m: ForwardMessage =>
      for {
        _ <- ZIO.logInfo("ForwardMessage")
        db <- ZIO.service[Ref[MediatorDB]]
        readAttachments = m.attachments match
          case Seq() => Left(FailToParse("ForwardMessage with no attachments"))
          case firstAttachment +: Seq() =>
            firstAttachment.data match {
              case AttachmentDataJWS(jws, links) =>
                Left(FailToParse("ForwardMessage of the type jws not implemented"))
              case AttachmentDataLinks(links, hash) =>
                Left(FailToParse("ForwardMessage of the type Links not implemented"))
              case AttachmentDataBase64(base64) =>
                base64.decodeToString.fromJson[EncryptedMessage] match
                  case Left(error) =>
                    Left(FailToParse(s"ForwardMessage does not contain a EncryptedMessage in Attachments: $error"))
                  case Right(nextMsg) => Right(nextMsg)
              case AttachmentDataJson(json) =>
                json.as[EncryptedMessage] match
                  case Left(error) =>
                    Left(FailToParse(s"ForwardMessage does not contain a EncryptedMessage in Attachments: $error"))
                  case Right(nextMsg) => Right(nextMsg)
              case AttachmentDataAny(jws, hash, links, base64, json) =>
                Left(FailToParse("ForwardMessage as Attachments of unknown type"))
            }
          case firstAttachments +: tail => Left(FailToParse("ForwardMessage with multi attachments"))
        _ <- readAttachments match
          case Left(error)    => ZIO.fail(error) // TODO return error msg
          case Right(nextMsg) => db.update(_.store(m.next, nextMsg))
      } yield None
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.none
      case Right(program) => program
  }

}
