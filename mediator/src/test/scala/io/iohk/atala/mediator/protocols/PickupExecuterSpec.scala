package io.iohk.atala.mediator.protocols

import fmgp.did.comm.Operations.*
import fmgp.did.comm.protocol.pickup3.*
import fmgp.did.comm.protocol.reportproblem2.{ProblemCode, ProblemReport}
import fmgp.did.comm.*
import fmgp.did.comm.protocol.pickup3.StatusRequest
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.{Agent, DIDSubject}
import fmgp.util.Base64
import io.iohk.atala.mediator.MediatorAgent
import io.iohk.atala.mediator.comm.MessageDispatcherJVMIOHK
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.db.AgentStub.*
import io.iohk.atala.mediator.db.EmbeddedMongoDBInstance.*
import io.iohk.atala.mediator.db.MessageItemRepoSpec.encryptedMessageAlice
import io.iohk.atala.mediator.protocols.*
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.indexes.{Index, IndexType}
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.http.Client
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

import scala.concurrent.ExecutionContext.Implicits.global

object PickupExecuterSpec extends ZIOSpecDefault with DidAccountStubSetup with MessageSetup {

  override def spec = {

    suite("PickupExecuterSpec")(
      test("Pickup Status message  should return ProblemReport") {
        val executer = PickupExecuter
        for {
          mediatorAgent <- ZIO.service[MediatorAgent]
          userAccount <- ZIO.service[UserAccountRepo]
          _ <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          _ <- userAccount.addAlias(
            owner = DIDSubject(aliceAgent.id.did),
            newAlias = DIDSubject(aliceAgent.id.did)
          )
          msg <- ZIO.fromEither(plaintextStatusMessage(aliceAgent.id.did, mediatorAgent.id.did))
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == ProblemReport.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("Pickup StatusRequest message  should return problem report for not enrolled did") {
        val executer = PickupExecuter
        for {
          mediatorAgent <- ZIO.service[MediatorAgent]
          userAccount <- ZIO.service[UserAccountRepo]
          msg <- ZIO.fromEither(plaintextStatusRequestMessage(aliceAgent.id.did, mediatorAgent.id.did))
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == ProblemReport.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("Pickup StatusRequest message  should return Status Message") {
        val executer = PickupExecuter
        for {
          mediatorAgent <- ZIO.service[MediatorAgent]
          userAccount <- ZIO.service[UserAccountRepo]
          _ <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          _ <- userAccount.addAlias(
            owner = DIDSubject(aliceAgent.id.did),
            newAlias = DIDSubject(aliceAgent.id.did)
          )
          msg <- ZIO.fromEither(plaintextStatusRequestMessage(aliceAgent.id.did, mediatorAgent.id.did))
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == Status.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("Pickup DeliveryRequest message  return MessageDelivery and attachment message") {
        val executer = PickupExecuter
        val forwardMessageExecuter = ForwardMessageExecuter
        for {
          mediatorAgent <- ZIO.service[MediatorAgent]
          userAccount <- ZIO.service[UserAccountRepo]
          _ <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          _ <- userAccount.addAlias(
            owner = DIDSubject(aliceAgent.id.did),
            newAlias = DIDSubject(aliceAgent.id.did)
          )
          basicMsg <- ZIO.fromEither(plainTextBasicMessage(bobAgent.id.did, aliceAgent.id.did))
          encryptedBasicMessage <- authEncrypt(basicMsg).provideSomeLayer(
            bobAgentLayer
          )
          msgForward <- ZIO.fromEither(
            plaintextForwardMessage(aliceAgent.id.did, mediatorAgent.id.did, encryptedBasicMessage.toJson)
          )
          result <- forwardMessageExecuter.execute(msgForward)
          msg <- ZIO.fromEither(
            plaintextDeliveryRequestMessage(aliceAgent.id.did, mediatorAgent.id.did, aliceAgent.id.did)
          )
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == MessageDelivery.piuri) && assertTrue(plainText.attachments.nonEmpty)
        }
      } @@ TestAspect.before(setupAndClean),
      test("Delivery Request message for Pickup returns a Status Message when there are no messages available") {
        val pickupExecuter = PickupExecuter
        for {
          mediatorAgent <- ZIO.service[MediatorAgent]
          userAccount <- ZIO.service[UserAccountRepo]
          _ <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          _ <- userAccount.addAlias(
            owner = DIDSubject(aliceAgent.id.did),
            newAlias = DIDSubject(aliceAgent.id.did)
          )
          msg <- ZIO.fromEither(
            plaintextDeliveryRequestMessage(aliceAgent.id.did, mediatorAgent.id.did, aliceAgent.id.did)
          )
          result <- pickupExecuter.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
        } yield {
          val plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          assertTrue(plainText.`type` == Status.piuri)
        }
      } @@ TestAspect.before(setupAndClean),
      test("Messages Received  message should clear the messages from the queue") {
        val executer = PickupExecuter
        val forwardMessageExecuter = ForwardMessageExecuter
        for {
          mediatorAgent <- ZIO.service[MediatorAgent]
          userAccount <- ZIO.service[UserAccountRepo]
          _ <- userAccount.createOrFindDidAccount(DIDSubject(aliceAgent.id.did))
          _ <- userAccount.addAlias(
            owner = DIDSubject(aliceAgent.id.did),
            newAlias = DIDSubject(aliceAgent.id.did)
          )
          basicMsg <- ZIO.fromEither(plainTextBasicMessage(bobAgent.id.did, aliceAgent.id.did))
          encryptedBasicMessage <- authEncrypt(basicMsg).provideSomeLayer(
            bobAgentLayer
          )
          msgForward <- ZIO.fromEither(
            plaintextForwardMessage(aliceAgent.id.did, mediatorAgent.id.did, encryptedBasicMessage.toJson)
          )
          result <- forwardMessageExecuter.execute(msgForward)
          msg <- ZIO.fromEither(
            plaintextDeliveryRequestMessage(aliceAgent.id.did, mediatorAgent.id.did, aliceAgent.id.did)
          )
          result <- executer.execute(msg)
          message <- ZIO.fromOption(result)
          decryptedMessage <- authDecrypt(message.asInstanceOf[EncryptedMessage]).provideSomeLayer(aliceAgentLayer)
          plainText = decryptedMessage.asInstanceOf[PlaintextMessage]
          attchmentID = plainText.attachments.map(_.flatMap(_.id).head).get
          messagesReceived <- ZIO.fromEither(
            plaintextMessagesReceivedRequestMessage(aliceAgent.id.did, mediatorAgent.id.did, attchmentID)
          )
          result <- executer.execute(messagesReceived)
        } yield {
          assertTrue(result.isEmpty)
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
