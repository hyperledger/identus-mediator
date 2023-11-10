package io.iohk.atala.mediator.protocols

import fmgp.did.comm.protocol.reportproblem2.{ProblemCode, ProblemReport}
import fmgp.did.comm.{EncryptedMessage, Operations, PlaintextMessage, SignedMessage, layerDefault}
import fmgp.did.method.peer.DidPeerResolver
import fmgp.util.Base64
import io.iohk.atala.mediator.comm.MessageDispatcherJVMIOHK
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.db.MessageItemRepoSpec.encryptedMessageAlice
import io.iohk.atala.mediator.protocols.*
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.http.Client
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import fmgp.did.{Agent, DIDSubject}
import io.iohk.atala.mediator.app.MediatorAgent

import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.bson.BSONDocument
import Operations.*
import fmgp.did.comm.protocol.mediatorcoordination2.*
import io.iohk.atala.mediator.db.AgentStub.*
import io.iohk.atala.mediator.db.EmbeddedMongoDBInstance.*
object MediatorCoordinationExecuterSpec extends ZIOSpecDefault with DidAccountStubSetup with MessageSetup {

  override def spec = {

    suite("MediatorCoordinationExecuterSpec")(
      test("MediationRequest message for enrolling new DID should get mediation grant") {
        val executer = MediatorCoordinationExecuter
        for {
          agent <- ZIO.service[MediatorAgent]
          msg <- ZIO.fromEither(plaintextMediationRequestMessage(bobAgent.id.did, agent.id.did))
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(bobAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == MediateGrant.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("MediationRequest message for already used alias did should get mediation deny") {
        val executer = MediatorCoordinationExecuter
        for {
          userAccount <- ZIO.service[UserAccountRepo]
          agent <- ZIO.service[MediatorAgent]
          result <- userAccount.createOrFindDidAccount(alice)
          result <- userAccount.addAlias(owner = alice, newAlias = DIDSubject(aliceAgent.id.did))
          msg <- ZIO.fromEither(plaintextMediationRequestMessage(aliceAgent.id.did, agent.id.did))
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == MediateDeny.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("KeyList Update message Request should add alias and return keyList Response") {
        val executer = MediatorCoordinationExecuter
        for {
          userAccount <- ZIO.service[UserAccountRepo]
          agent <- ZIO.service[MediatorAgent]
          result <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          msg <- ZIO.fromEither(
            plaintextKeyListUpdateRequestMessage(aliceAgent.id.did, agent.id.did, aliceAgent.id.did)
          )
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == KeylistResponse.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("KeyList remove alias message Request should remove alias and return keyList Response") {
        val executer = MediatorCoordinationExecuter
        for {
          userAccount <- ZIO.service[UserAccountRepo]
          agent <- ZIO.service[MediatorAgent]
          result <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          result <- userAccount.addAlias(owner = DIDSubject(aliceAgent.id.did), newAlias = DIDSubject(bobAgent.id.did))
          msg <- ZIO.fromEither(
            plaintextKeyListRemoveAliasRequestMessage(aliceAgent.id.did, agent.id.did, bobAgent.id.did)
          )
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == KeylistResponse.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("KeyList remove alias non existing didAccount Request should return problem report") {
        val executer = MediatorCoordinationExecuter
        for {
          userAccount <- ZIO.service[UserAccountRepo]
          agent <- ZIO.service[MediatorAgent]
          result <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          msg <- ZIO.fromEither(
            plaintextKeyListRemoveAliasRequestMessage(bobAgent.id.did, agent.id.did, bobAgent.id.did)
          )
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(bobAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          val problemReport = ProblemReport.fromPlaintextMessage(plainText)
          assert(problemReport)(
            isRight(
              hasField("code", (p: ProblemReport) => p.code, equalTo(ProblemCode.ErroFail("req", "not_enroll"))) &&
                hasField(
                  "from",
                  (p: ProblemReport) => p.from,
                  equalTo(agent.id.did)
                )
            )
          ) && assertTrue(plainText.`type` == ProblemReport.piuri)

        }
      } @@ TestAspect.before(setupAndClean)
    ).provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
      .provideSomeLayer(Operations.layerDefault)
      .provideSomeLayer(Scope.default >>> Client.default >>> MessageDispatcherJVMIOHK.layer)
      .provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
      .provideSomeLayer(AgentStub.agentLayer)
      .provideLayerShared(dataAccessLayer) @@ TestAspect.sequential
  }

  val dataAccessLayer = EmbeddedMongoDBInstance.layer(port, hostIp)
    >>> AsyncDriverResource.layer
    >>> ReactiveMongoApi.layer(connectionString)
    >>> (UserAccountRepo.layer ++ MessageItemRepo.layer ++ OutboxMessageRepo.layer)

}
