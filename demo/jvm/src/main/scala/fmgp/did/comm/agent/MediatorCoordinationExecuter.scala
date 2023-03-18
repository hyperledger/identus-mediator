package fmgp.did.comm.agent

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.Operations._
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.mediatorcoordination2._

/** Store all forwarded message */
case class MediatorDB(db: Map[DIDSubject, Seq[EncryptedMessage]] = Map.empty) {
  def isServing(subject: DIDSubject) = db.get(subject).isDefined
  def enroll(subject: DIDSubject) = MediatorDB(db.updatedWith(subject) {
    case Some(value) => Some(value)
    case None        => Some(Seq.empty)
  })
  def store(to: DIDSubject, msg: EncryptedMessage) = MediatorDB(db.updatedWith(to)(_.map(e => msg +: e)))
  def getMessages(to: DIDSubject, from: Option[DIDSubject]): Seq[EncryptedMessage] =
    val allMessageToDid = db.get(to).toSeq.flatten
    from match
      case None => allMessageToDid
      case Some(f) =>
        allMessageToDid.filter { case em =>
          em.`protected`.obj match
            case header: AuthProtectedHeader => header.skid.did == f
            case _                           => false

        }
}

object MediatorCoordinationExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services & Ref[MediatorDB]] {

  override def suportedPIURI: Seq[PIURI] = Seq(MediateRequest.piuri, MediateGrant.piuri, MediateDeny.piuri)

  override def program[R1 <: Ref[MediatorDB]](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Action] = {
    // the val is from the match to be definitely stable
    val piuriMediateRequest = MediateRequest.piuri
    val piuriMediateGrant = MediateGrant.piuri
    val piuriMediateDeny = MediateDeny.piuri

    (plaintextMessage.`type` match {
      case `piuriMediateRequest` => plaintextMessage.toMediateRequest
      case `piuriMediateGrant`   => plaintextMessage.toMediateGrant
      case `piuriMediateDeny`    => plaintextMessage.toMediateDeny
    }).map {
      case m: MediateGrant => ZIO.logWarning("MediateGrant") *> ZIO.succeed(NoReply)
      case m: MediateDeny  => ZIO.logWarning("MediateDeny") *> ZIO.succeed(NoReply)
      case m: MediateRequest =>
        for {
          _ <- ZIO.logInfo("MediateRequest")
          mediateGrant = m.makeRespondMediateGrant
          db <- ZIO.service[Ref[MediatorDB]]
          _ = db.update(_.enroll(mediateGrant.to.asDIDURL.toDID))
        } yield SyncReplyOnly(mediateGrant.toPlaintextMessage)
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program
  }

}
