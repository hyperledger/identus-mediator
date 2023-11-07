package io.iohk.atala.mediator.db

import fmgp.did.DIDSubject
import fmgp.did.comm.EncryptedMessage
import io.iohk.atala.mediator.StorageError
import reactivemongo.api.indexes.{Index, IndexType}
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.mediator.db.EmbeddedMongoDBInstance.*
import io.iohk.atala.mediator.protocols.MessageSetup

import scala.concurrent.ExecutionContext.Implicits.global
object UserAccountRepoSpec extends ZIOSpecDefault with DidAccountStubSetup with MessageSetup {

  override def spec = suite("UserAccountRepoSpec")(
    test("insert new Did Account") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        col <- userAccount.collection
        _ = col.indexesManager.create(index)
        result <- userAccount.createOrFindDidAccount(alice)
      } yield {
        assertTrue(result.isRight)
      }
    },
    test("insert same Did should NOT fail") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.createOrFindDidAccount(alice)
      } yield {
        assertTrue(result.isRight)
      }
    },
    test("Get Did Account") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.getDidAccount(alice)
      } yield {
        assertTrue(result.isDefined) && assertTrue(result.exists(_.did == alice))
      }
    },
    test("Get Did Account return for unknown did") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.getDidAccount(bob)
      } yield {
        assertTrue(result.isEmpty)
      }
    },
    test("Add alias to existing Did Account return right nModified value 1") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.addAlias(owner = alice, newAlias = bob)
        didAccount <- userAccount.getDidAccount(alice)
      } yield {
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(result.isRight) &&
        assertTrue(result == Right((1))) &&
        assertTrue(didAccount.isDefined) &&
        assertTrue(alias == Seq(bob))
      }
    },
    test("insert/create a UserAccount for a DID that is used as a alias should fail") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.createOrFindDidAccount(bob)
      } yield {
        assertTrue(result.isLeft)
      }
    },
    test("Add owner as alias to existing Did Account return right with nModified value 1") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.addAlias(owner = alice, newAlias = alice)
        didAccount <- userAccount.getDidAccount(alice)
      } yield {
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(result.isRight) &&
        assertTrue(result == Right(1)) &&
        assertTrue(didAccount.isDefined) &&
        assertTrue(alias.sorted == Seq(alice.did, bob.did).sorted)
      }
    },
    test("Add same alias to existing Did Account return right with nModified value 0") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.addAlias(owner = alice, newAlias = alice)
        didAccount <- userAccount.getDidAccount(alice)
      } yield {
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(result.isRight) &&
        assertTrue(result == Right(0)) &&
        assertTrue(didAccount.isDefined) &&
        assertTrue(alias.sorted == Seq(alice.did, bob.did).sorted)
      }
    },
    test("Remove alias to existing Did Account should return right with nModified value 1 ") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.removeAlias(alice, bob)
        didAccount <- userAccount.getDidAccount(alice)
      } yield {
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(result.isRight) &&
        assertTrue(result == Right(1)) &&
        assertTrue(didAccount.isDefined) &&
        assertTrue(alias == Seq(alice))
      }
    },
    test("Remove alias to unknown or unregister alias Did should return right with noModified value 0") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.removeAlias(alice, bob)
        didAccount <- userAccount.getDidAccount(alice)
      } yield {
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(result.isRight) &&
        assertTrue(result == Right(0)) &&
        assertTrue(didAccount.isDefined) &&
        assertTrue(alias == Seq(alice))
      }
    },
    test("addMessage to inbox for given Account") {
      val xRequestId = "b373423c-c78f-4cbc-a3fe-89cbc1351835"
      ZIO.logAnnotate(XRequestId.value, xRequestId) {
        for {
          userAccount <- ZIO.service[UserAccountRepo]
          messageItem <- ZIO.service[MessageItemRepo]
          result <- userAccount.addAlias(alice, bob)
          msg <- ZIO.fromEither(encryptedMessageAlice)
          msgAdded <- messageItem.insert(msg)
          addedToInbox <- userAccount.addToInboxes(Set(bob), msg)
          didAccount <- userAccount.getDidAccount(alice)
        } yield {
          val messageMetaData: Seq[MessageMetaData] = didAccount.map(_.messagesRef).getOrElse(Seq.empty)

          assertTrue(result.isRight) &&
          assertTrue(result == Right(1)) &&
          assertTrue(msgAdded.writeErrors == Nil) &&
          assertTrue(msgAdded.n == 1) &&
          assertTrue(addedToInbox == 1) &&
          assert(messageMetaData)(
            forall(
              hasField("hash", (m: MessageMetaData) => m.hash, equalTo(msg.sha1))
                && hasField("recipient", (m: MessageMetaData) => m.recipient, equalTo(bob))
                && hasField("xRequestId", (m: MessageMetaData) => m.xRequestId, equalTo(Some(xRequestId)))
            )
          )
        }
      }
    },
    test("mark message as delivered given did") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        msg <- ZIO.fromEither(encryptedMessageAlice)
        markedDelivered <- userAccount.markAsDelivered(alice, Seq(msg.sha1))
        didAccount <- userAccount.getDidAccount(alice)
      } yield {
        val messageMetaData: Seq[MessageMetaData] = didAccount.map(_.messagesRef).getOrElse(Seq.empty)
        assertTrue(markedDelivered == 1) &&
        assert(messageMetaData)(
          forall(
            hasField("state", (m: MessageMetaData) => m.state, equalTo(true))
          )
        )
      }
    },
  ).provideLayerShared(
    EmbeddedMongoDBInstance.layer(port, hostIp)
      >>> AsyncDriverResource.layer
      >>> ReactiveMongoApi.layer(connectionString)
      >>> (UserAccountRepo.layer ++ MessageItemRepo.layer)
  ) @@ TestAspect.sequential

}
