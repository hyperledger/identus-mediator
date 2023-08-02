package io.iohk.atala.mediator.protocols

import fmgp.did.comm.PlaintextMessage
import io.iohk.atala.mediator.MissingProtocolError
import io.iohk.atala.mediator.actions.ProtocolExecuter
import zio.ZIO

object NullProtocolExecuter extends ProtocolExecuter[Any] {

  override def suportedPIURI = Seq()
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) =
    ZIO.fail(MissingProtocolError(plaintextMessage.`type`))
}
