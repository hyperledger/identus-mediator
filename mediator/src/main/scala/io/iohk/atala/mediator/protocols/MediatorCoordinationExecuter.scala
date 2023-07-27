package io.iohk.atala.mediator.protocols

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.Operations.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.mediatorcoordination2.*
import io.iohk.atala.mediator.*
import io.iohk.atala.mediator.actions.*
import io.iohk.atala.mediator.db.UserAccountRepo
import zio.*
import zio.json.*
object MediatorCoordinationExecuter
    extends ProtocolExecuterWithServices[
      ProtocolExecuter.Services & UserAccountRepo,
      ProtocolExecuter.Erros
    ] {

  override def suportedPIURI: Seq[PIURI] = Seq(
    MediateRequest.piuri,
    MediateGrant.piuri,
    MediateDeny.piuri,
    KeylistUpdate.piuri,
    KeylistResponse.piuri,
    KeylistQuery.piuri,
    Keylist.piuri,
  )

  override def program[R1 <: (UserAccountRepo)](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, MediatorError | StorageError, Action] = {
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
      case m: MediateGrant =>
        ZIO.logWarning("MediateGrant") *> ZIO.succeed(NoReply) *>
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
      case m: MediateDeny =>
        ZIO.logWarning("MediateDeny") *> ZIO.succeed(NoReply) *>
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
      case m: MediateRequest =>
        for {
          _ <- ZIO.logInfo("MediateRequest")
          repo <- ZIO.service[UserAccountRepo]
          result <- repo.newDidAccount(m.from.asDIDURL.toDID)
          reply = result.n match
            case 1 => m.makeRespondMediateGrant.toPlaintextMessage
            case _ => m.makeRespondMediateDeny.toPlaintextMessage
        } yield SyncReplyOnly(reply)
      case m: KeylistUpdate =>
        for {
          _ <- ZIO.logInfo("KeylistUpdate")
          repo <- ZIO.service[UserAccountRepo]
          updateResponse <- ZIO.foreach(m.updates) {
            case (fromto, KeylistAction.add) =>
              repo.addAlias(m.from.toDID, fromto.toDID).map {
                case Left(value)     => (fromto, KeylistAction.add, KeylistResult.server_error)
                case Right(0)        => (fromto, KeylistAction.add, KeylistResult.no_change)
                case Right(newState) => (fromto, KeylistAction.add, KeylistResult.success)
              }
            case (fromto, KeylistAction.remove) =>
              repo.removeAlias(m.from.toDID, fromto.toDID).map {
                case Left(value)     => (fromto, KeylistAction.remove, KeylistResult.server_error)
                case Right(0)        => (fromto, KeylistAction.remove, KeylistResult.no_change)
                case Right(newState) => (fromto, KeylistAction.remove, KeylistResult.success)
              }
          }
        } yield SyncReplyOnly(m.makeKeylistResponse(updateResponse).toPlaintextMessage)
      case m: KeylistResponse =>
        ZIO.logWarning("KeylistResponse") *> ZIO.succeed(NoReply) *>
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
      case m: KeylistQuery =>
        for {
          _ <- ZIO.logInfo("KeylistQuery")
          repo <- ZIO.service[UserAccountRepo]
          mAccount <- repo.getDidAccount(m.from.toDID)
          mResponse = mAccount.map { account =>
            Keylist(
              thid = m.id,
              from = m.to.asFROM,
              to = m.from.asTO,
              keys = account.alias.map(e => Keylist.RecipientDID(e)),
              pagination = None,
            )
          }
        } yield mResponse match
          case None           => NoReply // TODO error report
          case Some(response) => SyncReplyOnly(response.toPlaintextMessage)
      case m: Keylist => ZIO.logWarning("Keylist") *> ZIO.succeed(NoReply)
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply) // TODO error report
      case Right(program) => program
  }

}
