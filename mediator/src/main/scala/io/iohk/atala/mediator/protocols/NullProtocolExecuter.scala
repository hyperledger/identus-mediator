package io.iohk.atala.mediator.protocols

import fmgp.did.comm.PlaintextMessage
import io.iohk.atala.mediator.MissingProtocolError
import io.iohk.atala.mediator.actions.ProtocolExecuterIOHK
import zio.ZIO
import fmgp.did.comm.SignedMessage
import fmgp.did.comm.EncryptedMessage

object NullProtocolExecuterIOHK extends ProtocolExecuterIOHK[Any, MissingProtocolError] {

  override def supportedPIURI = Seq()

  override def execute[Any](
      plaintextMessage: PlaintextMessage
  ): ZIO[Any, MissingProtocolError, Option[SignedMessage | EncryptedMessage]] =
    program(plaintextMessage).debug *> ZIO.none

  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) =
    ZIO.fail(MissingProtocolError(plaintextMessage.`type`))
}
