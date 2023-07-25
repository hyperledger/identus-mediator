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

trait ProtocolExecuter[-R] {

  def suportedPIURI: Seq[PIURI]

  /** @return can return a Sync Reply Msg */
  def execute[R1 <: R](plaintextMessage: PlaintextMessage): ZIO[R1, MediatorError, Option[EncryptedMessage]] =
    program(plaintextMessage) *> ZIO.none

  def program[R1 <: R](plaintextMessage: PlaintextMessage): ZIO[R1, MediatorError, Action]
}

object ProtocolExecuter {
  type Services = Resolver & Agent & Operations & MessageDispatcher
}
case class ProtocolExecuterCollection[-R <: Agent](executers: ProtocolExecuter[R]*) extends ProtocolExecuter[R] {

  override def suportedPIURI: Seq[PIURI] = executers.flatMap(_.suportedPIURI)

  def selectExecutersFor(piuri: PIURI) = executers.find(_.suportedPIURI.contains(piuri))

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, MediatorError, Option[EncryptedMessage]] =
    selectExecutersFor(plaintextMessage.`type`) match
      // case None     => NullProtocolExecuter.execute(plaintextMessage)
      case None     => MissingProtocolExecuter.execute(plaintextMessage)
      case Some(px) => px.execute(plaintextMessage)

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, MediatorError, Action] =
    selectExecutersFor(plaintextMessage.`type`) match
      // case None     => NullProtocolExecuter.program(plaintextMessage)
      case None     => MissingProtocolExecuter.program(plaintextMessage)
      case Some(px) => px.program(plaintextMessage)
}

trait ProtocolExecuterWithServices[-R <: ProtocolExecuter.Services] extends ProtocolExecuter[R] {

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, MediatorError, Option[EncryptedMessage]] =
    program(plaintextMessage)
      .tap(v => ZIO.logDebug(v.toString)) // DEBUG
      .flatMap(action => ActionUtils.packResponse(plaintextMessage, action))

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, MediatorError, Action]
}
