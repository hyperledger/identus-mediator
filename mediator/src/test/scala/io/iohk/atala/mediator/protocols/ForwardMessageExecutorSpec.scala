package io.iohk.atala.mediator.protocols

import fmgp.did.comm.protocol.reportproblem2.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.{EncryptedMessage, Operations, PlaintextMessage, SignedMessage, layerDefault}
import fmgp.did.method.peer.DidPeerResolver
import fmgp.util.Base64
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.db.MessageItemRepoSpec.encryptedMessageAlice
import io.iohk.atala.mediator.protocols.ForwardMessageExecuter
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.http.Client
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import fmgp.did.DIDSubject

import scala.concurrent.ExecutionContext.Implicits.global
import io.iohk.atala.mediator.db.EmbeddedMongoDBInstance.*
import reactivemongo.api.bson.BSONDocument

/** mediator/testOnly io.iohk.atala.mediator.protocols.ForwardMessageExecutorSpec */
object ForwardMessageExecutorSpec extends ZIOSpecDefault with DidAccountStubSetup with MessageSetup {

  override def spec = suite("ForwardMessageExecutorSpec")(
    test("Forward message for notEnrolled DID receives problem report ") {
      val executer = ForwardMessageExecuter
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.createOrFindDidAccount(alice)
        msg <- ZIO.fromEither(plaintextForwardNotEnrolledDidMessage)
        action <- executer.program(msg)
      } yield {
        action match
          case reply: AnyReply =>
            assert(reply.msg.toProblemReport)(
              isRight(
                hasField("code", (p: ProblemReport) => p.code, equalTo(ProblemCode.ErroFail("req", "not_enroll"))) &&
                  hasField("from", (p: ProblemReport) => p.from, equalTo(alice))
              )
            )
          case NoReply => assertTrue(false)
      }
    } @@ TestAspect.before(setupAndClean),
    test("Forward message for enrolled DID receives NoReply") {
      val executer = ForwardMessageExecuter
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.createOrFindDidAccount(alice)
        result <- userAccount.addAlias(owner = alice, newAlias = alice)
        msg <- ZIO.fromEither(plaintextForwardEnrolledDidMessage)
        action <- executer.program(msg)
      } yield action match
        case reply: AnyReply => assertTrue(false)
        case NoReply         => assertTrue(true)

    } @@ TestAspect.before(setupAndClean)
  ).provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
    .provideSomeLayer(Operations.layerDefault)
    .provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
    .provideSomeLayer(AgentStub.agentLayer)
    .provideLayerShared(dataAccessLayer) @@ TestAspect.sequential

  val dataAccessLayer = EmbeddedMongoDBInstance.layer(port, hostIp)
    >>> AsyncDriverResource.layer
    >>> ReactiveMongoApi.layer(connectionString)
    >>> (UserAccountRepo.layer ++ MessageItemRepo.layer ++ OutboxMessageRepo.layer)

}
