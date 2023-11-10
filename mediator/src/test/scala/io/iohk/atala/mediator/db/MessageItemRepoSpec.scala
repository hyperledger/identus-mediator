package io.iohk.atala.mediator.db

import fmgp.did.comm.EncryptedMessage
import io.iohk.atala.mediator.{DuplicateMessage, StorageError, StorageThrowable}
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.mediator.db.EmbeddedMongoDBInstance.*
import reactivemongo.core.errors.DatabaseException

object MessageItemRepoSpec extends ZIOSpecDefault with DidAccountStubSetup {

  override def spec = suite("MessageItemSpec")(
    test("insert message") {
      ZIO.logAnnotate(XRequestId.value, "b373423c-c78f-4cbc-a3fe-89cbc1351835") {
        for {
          messageItem <- ZIO.service[MessageItemRepo]

          msg <- ZIO.fromEither(encryptedMessageAlice)
          result <- messageItem.insert(msg)
        } yield {
          assertTrue(result.writeErrors == Nil) &&
          assertTrue(result.n == 1)
        }
      }
    },
    test("insert same message again") {
      ZIO.logAnnotate(XRequestId.value, "b373423c-c78f-4cbc-a3fe-89cbc1351835") {
        for {
          messageItem <- ZIO.service[MessageItemRepo]

          msg <- ZIO.fromEither(encryptedMessageAlice)
          result <- messageItem.insert(msg).exit
        } yield {
          assert(result)(fails(isSubtype[DuplicateMessage](anything)))
        }
      }
    },
    test("findById  message") {
      for {
        messageItem <- ZIO.service[MessageItemRepo]
        msg <- ZIO.fromEither(encryptedMessageAlice)
        result <- messageItem.findById(msg.sha256)
      } yield {
        val outcome = result.forall { messageItem =>
          messageItem.msg == msg &&
          messageItem._id == msg.sha256 &&
          messageItem.xRequestId.contains("b373423c-c78f-4cbc-a3fe-89cbc1351835")
        }
        assertTrue(outcome)
      }
    },
    test("findByIds messages") {
      ZIO.logAnnotate(XRequestId.value, "b373423c-c78f-4cbc-a3fe-89cbc1351835") {
        for {
          messageItem <- ZIO.service[MessageItemRepo]
          msg <- ZIO.fromEither(encryptedMessageAlice)
          msg2 <- ZIO.fromEither(encryptedMessageBob)
          msg2Added <- messageItem.insert(msg2)
          result <- messageItem.findByIds(Seq(msg.sha256, msg2.sha256))
        } yield {
          val outcome = result.forall { messageItem =>
            Seq(msg, msg2).contains(messageItem.msg) &&
            Seq(msg.sha256, msg2.sha256).contains(messageItem._id) &&
            messageItem.xRequestId.contains("b373423c-c78f-4cbc-a3fe-89cbc1351835")
          }
          assertTrue(outcome)
        }
      }
    }
  ).provideLayerShared(
    EmbeddedMongoDBInstance.layer(port, hostIp)
      >>> AsyncDriverResource.layer
      >>> ReactiveMongoApi.layer(connectionString)
      >>> MessageItemRepo.layer
  ) @@ TestAspect.sequential

}
