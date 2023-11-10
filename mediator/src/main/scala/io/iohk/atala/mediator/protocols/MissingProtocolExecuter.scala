package io.iohk.atala.mediator.protocols

import zio.ZIO

import fmgp.did.*
import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.protocol.Reply
import io.iohk.atala.mediator.MissingProtocolError
import io.iohk.atala.mediator.actions.ProtocolExecuterIOHK
import io.iohk.atala.mediator.actions.ProtocolExecuterIOHKWithServices
import io.iohk.atala.mediator.MediatorError

case class MissingProtocolExecuterIOHK()
    extends ProtocolExecuterIOHKWithServices[ProtocolExecuterIOHK.Services, MediatorError] {

  override def supportedPIURI = Seq()
  override def program[R1 <: Agent](plaintextMessage: PlaintextMessage) =
    ZIO
      .service[Agent]
      .map(agent =>
        Reply(
          Problems
            .unsupportedProtocolType(
              to = plaintextMessage.from.map(_.asTO).toSet,
              from = agent.id,
              pthid = plaintextMessage.id,
              piuri = plaintextMessage.`type`,
            )
            .toPlaintextMessage
        )
      )
}
