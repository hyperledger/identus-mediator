package org.hyperledger.identus.mediator.protocols

import zio.ZIO

import fmgp.did.*
import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.protocol.Reply
import org.hyperledger.identus.mediator.MissingProtocolError
import org.hyperledger.identus.mediator.MediatorError
import fmgp.did.comm.protocol.ProtocolExecuter

object MissingProtocolExecuter extends ProtocolExecuter[Agent, Nothing] {

  override def supportedPIURI = Seq()
  override def program(plaintextMessage: PlaintextMessage) =
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
