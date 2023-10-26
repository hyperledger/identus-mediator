package io.iohk.atala.mediator.protocols

import fmgp.did.DIDSubject
import fmgp.did.comm.protocol.reportproblem2.{ProblemCode, ProblemReport}
import fmgp.did.comm.protocol.discoverfeatures2._
import fmgp.did.comm.{EncryptedMessage, Operations, PlaintextMessage, SignedMessage, layerDefault}
import fmgp.did.method.peer.DidPeerResolver
import fmgp.util.Base64
import io.iohk.atala.mediator.comm.MessageDispatcherJVM
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.db.MessageItemRepoSpec.encryptedMessageAlice
import io.iohk.atala.mediator.protocols.ForwardMessageExecuter
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.http.Client
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

import scala.concurrent.ExecutionContext.Implicits.global
import io.iohk.atala.mediator.db.EmbeddedMongoDBInstance.*
import io.iohk.atala.mediator.protocols.MediatorCoordinationExecuterSpec.setupAndClean
import reactivemongo.api.bson.BSONDocument

object DiscoverFeaturesExecuterSpec extends ZIOSpecDefault with DidAccountStubSetup with MessageSetup {

  val plaintextDiscoverFeaturesQueryMessage = FeatureQuery(
    to = Set(alice.asTO),
    from = mediatorDid.asFROM,
    queries = ???,
  ).toPlaintextMessage.toFeatureQuery // Just to test the encode and parse

  override def spec = suite("DiscoverFeaturesExecuterSpec")(
    test("DiscoverFeatures Query message") {
      val executer = DiscoverFeaturesExecuter
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.createOrFindDidAccount(alice)
        msg <- ZIO.fromEither(plaintextDiscoverFeaturesQueryMessage)
        result <- executer.execute(msg)
        message <- ZIO.fromOption(result)
      } yield {
        assertTrue(message.isInstanceOf[SignedMessage])
        val signedMessage = message.asInstanceOf[SignedMessage]
        val jsonString = Base64.fromBase64url(signedMessage.payload.base64url).decodeToString
        val problemReport = jsonString.fromJson[PlaintextMessage].flatMap(ProblemReport.fromPlaintextMessage)
        assert(problemReport)(
          isRight(
            hasField("code", (p: ProblemReport) => p.code, equalTo(ProblemCode.ErroFail("req", "not_enroll"))) &&
              hasField(
                "from",
                (p: ProblemReport) => p.from,
                equalTo(alice)
              )
          )
        )
      }
    } @@ TestAspect.before(setupAndClean),

    // test("Forward message for enrolled DID receives NoReply") {
    //   val executer = ForwardMessageExecuter
    //   for {
    //     userAccount <- ZIO.service[UserAccountRepo]
    //     result <- userAccount.createOrFindDidAccount(alice)
    //     result <- userAccount.addAlias(owner = alice, newAlias = alice)
    //     msg <- ZIO.fromEither(plaintextForwardEnrolledDidMessage)
    //     result <- executer.execute(msg)
    //   } yield assertTrue(result.isEmpty)
    // } @@ TestAspect.before(setupAndClean)

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
