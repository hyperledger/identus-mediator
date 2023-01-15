package fmgp.did

import munit._

import fmgp.did._
import fmgp.crypto._

import zio._
import zio.json._
import zio.json.ast.Json
import zio.prelude.{Hash, Equal}

/** didJVM/testOnly fmgp.did.comm.HashSuite */
class HashSuite extends ZSuite with fmgp.util.AssertionsHash {

  test(s"Compere the equals '==' of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assertEquals(a, b)
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

  test(s"Compere the hashCode of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assertEquals(a.hashCode, b.hashCode)
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

  test(s"Compere the Equal of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assert(Hash[EncryptedMessage].equal(a, b))
          assert(Equal[EncryptedMessage].equal(a, b))
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

  test(s"Compere the Hash of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assertEquals(Hash[EncryptedMessage].hash(a), Hash[EncryptedMessage].hash(b))
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

}
