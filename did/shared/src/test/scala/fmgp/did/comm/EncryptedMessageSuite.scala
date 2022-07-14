package fmgp.did.comm

import munit._
import zio.json._
import fmgp.did.DIDDocument

class EncryptedMessageSuite extends FunSuite {

  test("Example parse plaintextMessage") {
    val ret = EncryptedMessageExamples.plaintextMessage.fromJson[PlaintextMessageClass]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.`type`, "https://example.com/protocols/lets_do_lunch/1.0/proposal")
    }
  }

  test("Example parse encryptedMessage_ECDH1PU_X25519_A256CBCHS512") {
    val ret = EncryptedMessageExamples.encryptedMessage_ECDH1PU_X25519_A256CBCHS512.fromJson[EncryptedMessageGeneric]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  =>
    }
  }

  EncryptedMessageExamples.allEncryptedMessage.zipWithIndex.foreach((example, index) =>
    test(s"Example parse Encrypted Messages (index $index)") {
      val ret = example.fromJson[EncryptedMessageGeneric]
      ret match {
        case Left(error) => fail(error)
        case Right(obj)  => assert(!obj.recipients.isEmpty)
      }
    }
  )

}
