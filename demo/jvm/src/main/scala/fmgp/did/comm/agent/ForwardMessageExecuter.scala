package fmgp.did.comm.agent

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.routing2._

object ForwardMessageExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services & Ref[MediatorDB]] {

  override def suportedPIURI: Seq[PIURI] = Seq(ForwardMessage.piuri)

  override def program[R1 <: Ref[MediatorDB]](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Action] = {
    // the val is from the match to be definitely stable
    val piuriForwardMessage = ForwardMessage.piuri

    (plaintextMessage.`type` match {
      case `piuriForwardMessage` => plaintextMessage.toForwardMessage
    }).map { case m: ForwardMessage =>
      for {
        _ <- ZIO.logInfo("ForwardMessage")
        db <- ZIO.service[Ref[MediatorDB]]
        _ <- db.update(_.store(m.next, m.msg))
      } yield None
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program *> ZIO.succeed(NoReply)
  }

}
