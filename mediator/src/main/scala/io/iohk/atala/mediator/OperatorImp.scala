package io.iohk.atala.mediator

import zio.*

import fmgp.did.*
import fmgp.util.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol._
import fmgp.crypto.error.DidFail

object OperatorImp {
  // val basicProtocolHandlerLayer: ULayer[ProtocolExecuter[ProtocolExecuter.Services, DidFail]] =
  //   ZLayer.succeed(
  //     ProtocolExecuterCollection(
  //       BasicMessageExecuter,
  //       new TrustPingExecuter,
  //     )(NullProtocolExecute)
  //   )

  // val layer: ZLayer[Any, Nothing, Operator] =
  val layer =
    AgentExecutarImp.basicProtocolHandlerLayer >>>
      ZLayer.fromZIO(
        for {
          protocolHandler <- ZIO.service[ProtocolExecuter[Resolver & Agent & Operations, DidFail]]
          protocolHandlerAux = protocolHandler.mapError(didFail => MediatorDidError(didFail))
          mediator <- ZIO.service[MediatorAgent]
          self <- AgentExecutorMediator.make(mediator, protocolHandlerAux)
          _ <- ZIO.log("Operator: " + self.subject.toString)
          operator = Operator(
            selfOperator = self,
            contacts = Seq(self)
          )
        } yield operator
      )

}
