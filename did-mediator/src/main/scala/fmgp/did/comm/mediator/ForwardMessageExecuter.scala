package fmgp.did.comm.mediator

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.routing2._
import fmgp.did.db._

object ForwardMessageExecuter
    extends ProtocolExecuterWithServices[
      ProtocolExecuter.Services & Ref[MediatorDB] & DidAccountRepo & MessageItemRepo
    ] {

  override def suportedPIURI: Seq[PIURI] = Seq(ForwardMessage.piuri)

  override def program[R1 <: Ref[MediatorDB] & DidAccountRepo & MessageItemRepo](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Action] = {
    // the val is from the match to be definitely stable
    val piuriForwardMessage = ForwardMessage.piuri

    (plaintextMessage.`type` match {
      case `piuriForwardMessage` => plaintextMessage.toForwardMessage
    }).map { case m: ForwardMessage =>
      for {
        _ <- ZIO.logInfo("ForwardMessage")
        repoMessageItem <- ZIO.service[MessageItemRepo]
        repoDidAccount <- ZIO.service[DidAccountRepo]
        // next = m.next
        msg = MessageItem(m.msg)
        aaa = msg
        aa = repoMessageItem.insertOne(msg)

        // db <- ZIO.service[Ref[MediatorDB]]
        // _ <- db.update(_.store(m.next, m.msg))
      } yield None
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program *> ZIO.succeed(NoReply)
  }

}
