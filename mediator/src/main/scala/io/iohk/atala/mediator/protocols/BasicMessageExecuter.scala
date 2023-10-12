package io.iohk.atala.mediator.protocols

import fmgp.crypto.error.FailToParse
import fmgp.did.comm.{PIURI, PlaintextMessage}
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import io.iohk.atala.mediator.{MediatorError, MediatorDidError, MediatorThrowable}
import io.iohk.atala.mediator.actions.{NoReply, ProtocolExecuter}
import zio.{Console, ZIO}

object BasicMessageExecuter extends ProtocolExecuter[Any, MediatorError] {

  override def supportedPIURI: Seq[PIURI] = Seq(BasicMessage.piuri)
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) = for {
    job <- BasicMessage.fromPlaintextMessage(plaintextMessage) match
      case Left(error) => ZIO.fail(MediatorDidError(FailToParse(error)))
      case Right(bm)   => Console.printLine(bm.toString).mapError(ex => MediatorThrowable(ex))
  } yield NoReply

}
