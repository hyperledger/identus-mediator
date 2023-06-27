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

import scala.concurrent.ExecutionContext.Implicits.global
object UserAccountRepoSpec extends ZIOSpecDefault with AccountStubSetup {
  val port = 27778
  val hostIp = "localhost"

  val connectionString = s"mongodb://$hostIp:$port/messages"
  // Define the index
  val index = Index(
    key = Seq("alias" -> IndexType.Ascending),
    name = Some("alias_did"),
    unique = true,
    background = true
  )

  override def spec = suite("UserAccountRepoSpec")(
    test("insert new Did Account") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        col <- userAccount.collection
        _ = col.indexesManager.create(index)
        result <- userAccount.newDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(result.writeErrors == Nil)
        assertTrue(result.n == 1)
      }
    },
    test("insert same Did should fail") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.newDidAccount(DIDSubject(alice)).exit
      } yield {
        assert(result)(fails(isSubtype[StorageError](anything)))
      }
    },
    test("Get Did Account") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.getDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(result.isDefined)
        assertTrue(result.exists(_.did == DIDSubject(alice)))
      }
    },
    test("Get Did Account return for unknown did") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.getDidAccount(DIDSubject(bob))
      } yield {
        assertTrue(result.isEmpty)
      }
    },
    test("Add alias to existing Did Account return right nModified value 1") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.addAlias(DIDSubject(alice), DIDSubject(bob))
        didAccount <- userAccount.getDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(result.isRight)
        assertTrue(result == Right((1)))
        assertTrue(didAccount.isDefined)
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(alias == Seq(alice, bob))
      }
    },
    test("Add same alias to existing Did Account return right with nModified value 0") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.addAlias(DIDSubject(alice), DIDSubject(alice))
        didAccount <- userAccount.getDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(result.isRight)
        assertTrue(result == Right(0))
        assertTrue(didAccount.isDefined)
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(alias == Seq(alice, bob))
      }
    },
    test("Remove alias to existing Did Account should return right with nModified value 1 ") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.removeAlias(DIDSubject(alice), DIDSubject(bob))
        didAccount <- userAccount.getDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(result.isRight)
        assertTrue(result == Right(1))
        assertTrue(didAccount.isDefined)
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(alias == Seq(alice))
      }
    },
    test("Remove alias to unknown or unregister alias Did  should return right with noModified value 0") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        result <- userAccount.removeAlias(DIDSubject(alice), DIDSubject(bob))
        didAccount <- userAccount.getDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(result.isRight)
        assertTrue(result == Right(0))
        assertTrue(didAccount.isDefined)
        val alias: Seq[String] = didAccount.map(_.alias.map(_.did)).getOrElse(Seq.empty)
        assertTrue(alias == Seq(alice))
      }
    },
    test("addMessage to inbox for given Account") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        messageItem <- ZIO.service[MessageItemRepo]
        result <- userAccount.addAlias(DIDSubject(alice), DIDSubject(bob))
        msg <- ZIO.fromEither(encryptedMessageAlice)
        msgAdded <- messageItem.insert(MessageItem(msg))
        addedToInbox <- userAccount.addToInboxes(Set(DIDSubject(bob)), msg)
        didAccount <- userAccount.getDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(result.isRight)
        assertTrue(result == Right(1))
        assertTrue(msgAdded.writeErrors == Nil)
        assertTrue(msgAdded.n == 1)
        assertTrue(addedToInbox == 1)
        val messageMetaData: Seq[MessageMetaData] = didAccount.map(_.messagesRef).getOrElse(Seq.empty)
        assert(messageMetaData)(
          forall(
            hasField("hash", (m: MessageMetaData) => m.hash, equalTo(msg.hashCode()))
              && hasField("recipient", (m: MessageMetaData) => m.recipient, equalTo(DIDSubject(bob)))
          )
        )
      }
    },
    test("mark message as delivered given did") {
      for {
        userAccount <- ZIO.service[UserAccountRepo]
        msg <- ZIO.fromEither(encryptedMessageAlice)
        markedDelivered <- userAccount.markAsDelivered(DIDSubject(alice), Seq(msg.hashCode()))
        didAccount <- userAccount.getDidAccount(DIDSubject(alice))
      } yield {
        assertTrue(markedDelivered == 1)
        val messageMetaData: Seq[MessageMetaData] = didAccount.map(_.messagesRef).getOrElse(Seq.empty)
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
