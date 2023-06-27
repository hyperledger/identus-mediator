package io.iohk.atala.mediator.db

import fmgp.did.comm.EncryptedMessage
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object MessageItemRepoSpec extends ZIOSpecDefault with AccountStubSetup {
  val port = 27777
  val hostIp = "localhost"
  val connectionString = s"mongodb://$hostIp:$port/messages"

  override def spec = suite("MessageItemSpec")(
    test("insert message") {
      for {
        messageItem <- ZIO.service[MessageItemRepo]
        msg <- ZIO.fromEither(encryptedMessageAlice)
        result <- messageItem.insert(MessageItem(msg))
      } yield {
        println(result)
        assertTrue(result.writeErrors == Nil)
        assertTrue(result.n == 1)
      }
    },
    test("findById  message") {
      for {
        messageItem <- ZIO.service[MessageItemRepo]
        msg <- ZIO.fromEither(encryptedMessageAlice)
        result <- messageItem.findById(msg.hashCode())
      } yield {
        assertTrue(result.contains(MessageItem(msg)))
      }
    },
    test("findByIds messages") {
      for {
        messageItem <- ZIO.service[MessageItemRepo]
        msg <- ZIO.fromEither(encryptedMessageAlice)
        msg2 <- ZIO.fromEither(encryptedMessageBob)
        msg2Added <- messageItem.insert(MessageItem(msg2))
        result <- messageItem.findByIds(Seq(msg.hashCode(), msg2.hashCode()))
      } yield {
        assertTrue(result.contains(MessageItem(msg)))
        assertTrue(result.contains(MessageItem(msg2)))
      }
    }
  ).provideLayerShared(
    EmbeddedMongoDBInstance.layer(port, hostIp)
      >>> AsyncDriverResource.layer
      >>> ReactiveMongoApi.layer(connectionString)
      >>> MessageItemRepo.layer
  ) @@ TestAspect.sequential

}
