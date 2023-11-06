package io.iohk.atala.mediator.protocols

import fmgp.did.DIDSubject
import fmgp.did.comm.protocol.reportproblem2.{ProblemCode, ProblemReport}
import fmgp.did.comm.protocol.discoverfeatures2.*
import fmgp.did.comm.{EncryptedMessage, Operations, PlaintextMessage, SignedMessage, layerDefault}
import fmgp.did.method.peer.DidPeerResolver
import fmgp.util.Base64
import io.iohk.atala.mediator.comm.MessageDispatcherJVM
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.db.MessageItemRepoSpec.encryptedMessageAlice
import io.iohk.atala.mediator.protocols.DiscoverFeaturesExecuter
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.http.Client
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import fmgp.did.comm.FROM
import fmgp.did.comm.TO

import scala.concurrent.ExecutionContext.Implicits.global
import io.iohk.atala.mediator.db.EmbeddedMongoDBInstance.*
import reactivemongo.api.bson.BSONDocument
import fmgp.did.DIDSubject.*
import fmgp.did.comm.Operations.authDecrypt
import io.iohk.atala.mediator.app.MediatorAgent
import io.iohk.atala.mediator.db.AgentStub.{bobAgent, bobAgentLayer}
object DiscoverFeaturesExecuterSpec extends ZIOSpecDefault with DidAccountStubSetup with MessageSetup {

  override def spec = suite("DiscoverFeaturesExecuterSpec")(
    test("DiscoverFeatures Query message") {
      val executer = DiscoverFeaturesExecuter
      for {
        agent <- ZIO.service[MediatorAgent]
        msg <- ZIO.fromEither(plaintextDiscoverFeatureRequestMessage(bobAgent.id.did, agent.id.did))
        result <- executer.execute(msg)
        message <- ZIO.fromOption(result)
        decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(bobAgentLayer)

      } yield {
        val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
        assertTrue(plainText.`type` == FeatureDisclose.piuri)
      }
    } @@ TestAspect.before(setupAndClean),
  ).provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
    .provideSomeLayer(Operations.layerDefault)
    .provideSomeLayer(Scope.default >>> Client.default >>> MessageDispatcherJVM.layer)
    .provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
    .provideSomeLayer(AgentStub.agentLayer)
    .provideLayerShared(dataAccessLayer) @@ TestAspect.sequential

  val dataAccessLayer = EmbeddedMongoDBInstance.layer(port, hostIp)
    >>> AsyncDriverResource.layer
    >>> ReactiveMongoApi.layer(connectionString)
    >>> (UserAccountRepo.layer ++ MessageItemRepo.layer ++ OutboxMessageRepo.layer)

}
