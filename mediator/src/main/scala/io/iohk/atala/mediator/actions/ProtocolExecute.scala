package io.iohk.atala.mediator.actions

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.Operations.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.basicmessage2.*
import fmgp.did.comm.protocol.trustping2.*
import io.iohk.atala.mediator.*
import io.iohk.atala.mediator.comm.*
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.protocols.NullProtocolExecuter
import zio.*
import zio.json.*
import io.iohk.atala.mediator.protocols.MissingProtocolExecuter
//TODO pick a better name // maybe "Protocol" only

trait ProtocolExecuter[-R, +E] { // <: MediatorError | StorageError] {

  def suportedPIURI: Seq[PIURI]

  /** @return can return a Sync Reply Msg */
  def execute[R1 <: R](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, E, Option[EncryptedMessage]] =
    program(plaintextMessage) *> ZIO.none

  def program[R1 <: R](plaintextMessage: PlaintextMessage): ZIO[R1, E, Action]
}

object ProtocolExecuter {
  type Services = Resolver & Agent & Operations & MessageDispatcher
  type Erros = MediatorError | StorageError
}
case class ProtocolExecuterCollection[-R <: Agent, +E](
    executers: ProtocolExecuter[R, E]*
) extends ProtocolExecuter[R, E] {

  override def suportedPIURI: Seq[PIURI] = executers.flatMap(_.suportedPIURI)

  def selectExecutersFor(piuri: PIURI) = executers.find(_.suportedPIURI.contains(piuri))

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, E, Option[EncryptedMessage]] =
    selectExecutersFor(plaintextMessage.`type`) match
      // case None     => NullProtocolExecuter.execute(plaintextMessage)
      case None     => MissingProtocolExecuter.execute(plaintextMessage)
      case Some(px) => px.execute(plaintextMessage)

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, E, Action] =
    selectExecutersFor(plaintextMessage.`type`) match
      // case None     => NullProtocolExecuter.program(plaintextMessage)
      case None     => MissingProtocolExecuter.program(plaintextMessage)
      case Some(px) => px.program(plaintextMessage)
}

trait ProtocolExecuterWithServices[
    -R <: ProtocolExecuter.Services,
    +E >: MediatorError // ProtocolExecuter.Erros
] extends ProtocolExecuter[R, E] {

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, E, Option[EncryptedMessage]] =
    program(plaintextMessage)
      .tap(v => ZIO.logDebug(v.toString)) // DEBUG
      .flatMap(action => ActionUtils.packResponse(Some(plaintextMessage), action))

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, E, Action]
}
