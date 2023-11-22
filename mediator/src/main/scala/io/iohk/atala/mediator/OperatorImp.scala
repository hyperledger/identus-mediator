package io.iohk.atala.mediator

import zio.*

import fmgp.crypto.error.DidFail
import fmgp.did.*
import fmgp.util.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.*
import fmgp.did.framework.*
import io.iohk.atala.mediator.protocols.*
import io.iohk.atala.mediator.db.{UserAccountRepo, MessageItemRepo}

object OperatorImp {
  type Services = Resolver & Agent & Operations & UserAccountRepo & MessageItemRepo

  val protocolHandlerLayer: ULayer[
    ProtocolExecuter[Services, MediatorError | StorageError]
  ] =
    ZLayer.succeed(
      ProtocolExecuterCollection(
        BasicMessageExecuter.mapError(didFail => ProtocolExecutionFailToParse(didFail)),
        (new TrustPingExecuter).mapError(didFail => ProtocolExecutionFailToParse(didFail)),
        DiscoverFeaturesExecuter,
        MediatorCoordinationExecuter,
        ForwardMessageExecuter,
        PickupExecuter
      )(MissingProtocolExecuter) // (NullProtocolExecute.mapError(didFail => ProtocolExecutionFailToParse(didFail)))
    )

  val layer: ZLayer[MediatorAgent & UserAccountRepo & MessageItemRepo & TransportFactory, Nothing, Operator] =
    protocolHandlerLayer >>>
      ZLayer.fromZIO(
        for {
          protocolHandlerAux <- ZIO.service[ProtocolExecuter[Services, MediatorError | StorageError]]
          mediator <- ZIO.service[MediatorAgent]
          userAccountRepo <- ZIO.service[UserAccountRepo]
          messageItemRepo <- ZIO.service[MessageItemRepo]
          self <- AgentExecutorMediator.make(mediator, protocolHandlerAux, userAccountRepo, messageItemRepo)
          _ <- ZIO.log("Operator: " + self.subject.toString)
          operator = Operator(
            selfOperator = self,
            contacts = Seq(self)
          )
        } yield operator
      )

}
