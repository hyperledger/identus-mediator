package io.iohk.atala.mediator.protocols

import zio.ZIO

import fmgp.did.*
import fmgp.did.comm.PlaintextMessage
import io.iohk.atala.mediator.MissingProtocolError
import io.iohk.atala.mediator.actions.ProtocolExecuter
import io.iohk.atala.mediator.actions.Reply

object MissingProtocolExecuter extends ProtocolExecuter[Agent, Nothing] {

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
