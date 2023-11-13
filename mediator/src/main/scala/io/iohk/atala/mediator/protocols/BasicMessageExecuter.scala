package io.iohk.atala.mediator.protocols

import zio._

import fmgp.crypto.error._
import fmgp.did.comm.{EncryptedMessage, PIURI, PlaintextMessage, SignedMessage}
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import io.iohk.atala.mediator.{MediatorDidError, MediatorError, MediatorThrowable, StorageError}
import io.iohk.atala.mediator.actions.ProtocolExecuterIOHK

//TODO REMOVE
object BasicMessageExecuterIOHK extends ProtocolExecuterIOHK[Any, MediatorError] {

  override def supportedPIURI: Seq[PIURI] = Seq(BasicMessage.piuri)

  override def execute[R](
      plaintextMessage: PlaintextMessage
  ): ZIO[R, MediatorError, Option[SignedMessage | EncryptedMessage]] =
    program(plaintextMessage).debug *> ZIO.none

  override def program[R1 <: Any](plaintextMessage: PlaintextMessage): ZIO[R1, MediatorError, Action] = for {
    job <- BasicMessage.fromPlaintextMessage(plaintextMessage) match
      case Left(error) => ZIO.fail(MediatorDidError(FailToParse(error)))
      case Right(bm)   => Console.printLine(bm.toString).mapError(ex => MediatorThrowable(ex))
  } yield NoReply

}
