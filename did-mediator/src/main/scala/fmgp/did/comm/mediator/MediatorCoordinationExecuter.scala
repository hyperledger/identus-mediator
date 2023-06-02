package fmgp.did.comm.mediator

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.Operations._
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.mediatorcoordination2._
import fmgp.did.db.DidAccountRepo

/** Store all forwarded message */
case class MediatorDB(db: Map[DIDSubject, Seq[EncryptedMessage]], alias: Map[DIDSubject, DIDSubject]) {
  def isServing(subject: DIDSubject) = db.get(subject).isDefined
  def enroll(subject: DIDSubject): Either[String, MediatorDB] =
    alias.keys.find(_.string == subject.string) match
      case Some(value) => Left(s"${subject.string} is alredy used as a alias for ${value.string}")
      case None =>
        Right(
          this.copy(
            db = db.updatedWith(subject) {
              case Some(value) => Some(value)
              case None        => Some(Seq.empty)
            }
          )
        )
  def addAlias(ower: DIDSubject, newAlias: DIDSubject) =
    db.keys.find(_ == newAlias) match
      case Some(did) => Left(s"${did} is alredy enrolled for mediation ")
      case None =>
        alias.find(_._1 == newAlias) match
          case Some((a, ower)) => Left(s"$newAlias is alredy an alias of $ower")
          case None            => Right(this.copy(alias = alias + (newAlias -> ower)))
  def removeAlias(ower: DIDSubject, newAlias: DIDSubject) =
    alias.find(_._1 == newAlias) match
      case None                                           => Left(s"$newAlias is not on DB")
      case Some((oldAlias, oldOwer)) if (oldOwer != ower) => Left(s"$newAlias is not owed by $ower")
      case Some((oldAlias, oldOwer)) => Right(this.copy(alias = alias.view.filterKeys(_ == newAlias).toMap))

  def store(to: DIDSubject, msg: EncryptedMessage) =
    MediatorDB(
      db = db.updatedWith(alias.getOrElse(to, to))(_.map(e => msg +: e)),
      alias = alias
    )

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

object MediatorDB {
  def empty = MediatorDB(db = Map.empty, alias = Map.empty)
}

object MediatorCoordinationExecuter
    extends ProtocolExecuterWithServices[ProtocolExecuter.Services & Ref[MediatorDB] & DidAccountRepo] {

  override def suportedPIURI: Seq[PIURI] = Seq(
    MediateRequest.piuri,
    MediateGrant.piuri,
    MediateDeny.piuri,
    KeylistUpdate.piuri,
    KeylistResponse.piuri,
    KeylistQuery.piuri,
    Keylist.piuri,
  )

  override def program[R1 <: (Ref[MediatorDB] & DidAccountRepo)](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Action] = {
    // the val is from the match to be definitely stable
    val piuriMediateRequest = MediateRequest.piuri
    val piuriMediateGrant = MediateGrant.piuri
    val piuriMediateDeny = MediateDeny.piuri
    val piuriKeylistUpdate = KeylistUpdate.piuri
    val piuriKeylistResponse = KeylistResponse.piuri
    val piuriKeylistQuery = KeylistQuery.piuri
    val piuriKeylist = Keylist.piuri

    (plaintextMessage.`type` match {
      case `piuriMediateRequest`  => plaintextMessage.toMediateRequest
      case `piuriMediateGrant`    => plaintextMessage.toMediateGrant
      case `piuriMediateDeny`     => plaintextMessage.toMediateDeny
      case `piuriKeylistUpdate`   => plaintextMessage.toKeylistUpdate
      case `piuriKeylistResponse` => plaintextMessage.toKeylistResponse
      case `piuriKeylistQuery`    => plaintextMessage.toKeylistQuery
      case `piuriKeylist`         => plaintextMessage.toKeylist
    }).map {
      case m: MediateGrant => ZIO.logWarning("MediateGrant") *> ZIO.succeed(NoReply)
      case m: MediateDeny  => ZIO.logWarning("MediateDeny") *> ZIO.succeed(NoReply)
      case m: MediateRequest =>
        for {
          _ <- ZIO.logInfo("MediateRequest")
          // db <- ZIO.service[Ref[MediatorDB]]
          // reply <- db.modify { db =>
          //   db.enroll(m.from.asDIDURL.toDID) match
          //     case Left(fail)   => (m.makeRespondMediateDeny.toPlaintextMessage, db)
          //     case Right(newDB) => (m.makeRespondMediateGrant.toPlaintextMessage, newDB)
          // }
          repo <- ZIO.service[DidAccountRepo]
          result <- repo.newDidAccount(m.from.asDIDURL.toDID)
          reply = result.n match
            case 1 => m.makeRespondMediateGrant.toPlaintextMessage
            case _ => m.makeRespondMediateDeny.toPlaintextMessage
        } yield SyncReplyOnly(reply)
      case m: KeylistUpdate =>
        case class Tmp(id: FROMTO, a: KeylistAction, r: KeylistResult)
        for {
          _ <- ZIO.logInfo("KeylistUpdate")
          repo <- ZIO.service[DidAccountRepo]
          // TODO!!!!!!!!!!!!!!!!!!!!
          db <- ZIO.service[Ref[MediatorDB]]
          updatesAndNewMediatorDB <- db.modify { mediatorDB =>
            val did2Add = m.updates.collect { case (fromto, KeylistAction.add) => fromto }
            val did2Remove = m.updates.collect { case (fromto, KeylistAction.remove) => fromto }
            m.updates.foldLeft((Seq.empty[Tmp], mediatorDB)) {
              case ((resultList, tmpDB), (fromto, KeylistAction.add)) =>
                tmpDB.addAlias(ower = m.from.toDIDSubject, newAlias = fromto.toDIDSubject) match
                  case Left(value) =>
                    (
                      resultList :+ Tmp(fromto, KeylistAction.add, KeylistResult.server_error),
                      tmpDB
                    )
                  case Right(newState) =>
                    (
                      resultList :+ Tmp(fromto, KeylistAction.add, KeylistResult.success),
                      newState
                    )

              case ((resultList, tmpDB), (fromto, KeylistAction.remove)) =>
                tmpDB.removeAlias(ower = m.from.toDIDSubject, newAlias = fromto.toDIDSubject) match
                  case Left(value) =>
                    (
                      resultList :+ Tmp(fromto, KeylistAction.remove, KeylistResult.server_error),
                      tmpDB
                    )
                  case Right(newState) =>
                    (
                      resultList :+ Tmp(fromto, KeylistAction.remove, KeylistResult.success),
                      newState
                    )
            }
          }
          keylistResponse = m.makeKeylistResponse(
            updatesAndNewMediatorDB.map(e => (e._1, e._2, e._3))
          )
        } yield SyncReplyOnly(keylistResponse.toPlaintextMessage)
      case m: KeylistResponse => ZIO.logWarning("KeylistResponse") *> ZIO.succeed(NoReply)
      case m: KeylistQuery    => ZIO.logError("Not implemented KeylistQuery") *> ZIO.succeed(NoReply) // TODO
      case m: Keylist         => ZIO.logWarning("Keylist") *> ZIO.succeed(NoReply)
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program
  }

}
