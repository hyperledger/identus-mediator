package fmgp.did.comm

import munit._
import zio.json._
import fmgp.did.DIDDocument
import fmgp.crypto._

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
      case Right(obj)  => // ok
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

  // ###############
  // ### decrypt ###
  // ###############

  val expeted = PlaintextMessageClass(
    "1234567890",
    "http://example.com/protocols/lets_do_lunch/1.0/proposal",
    Some(Set("did:example:bob")),
    Some("did:example:alice"),
    None,
    Some(1516269022),
    Some(1516385931),
    Map("messagespecificattribute" -> "and its value")
  )

  test("decrypt encryptedMessage_ECDHES_X25519_XC20P".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_ECDHES_X25519_XC20P.fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(messages)) =>
        assertEquals(ks.keys.size, 9)

        Future.sequence(messages.recipients.dropRight(2).map { recipient => // FIXME REMOVE dropRight(2)
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, messages).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, expeted)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  test("decrypt encryptedMessage_ECDHES_P384_A256CBCHS512") {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_ECDHES_P384_A256CBCHS512.fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(messages)) =>
        assertEquals(ks.keys.size, 9)

        Future.sequence(messages.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, messages).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, expeted)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  test("decrypt encryptedMessage_ECDHES_P521_A256GCM") {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_ECDHES_P521_A256GCM.fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(messages)) =>
        assertEquals(ks.keys.size, 9)

        Future.sequence(messages.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, messages).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, expeted)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  test("decrypt encryptedMessage_ECDH1PU_X25519_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_ECDH1PU_X25519_A256CBCHS512.fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(messages)) =>
        assertEquals(ks.keys.size, 9)

        val signbyKey = JWKExamples.senderKeyX25519
          .fromJson[OKPPublicKey]
          .toOption
          .get

        Future.sequence(messages.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decryptAndVerify(key.get, signbyKey, recipient.encrypted_key, messages).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj) =>
                assertEquals(obj, expeted)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  test("decrypt encryptedMessage_EdDSA_ECDH1PU_P521_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_EdDSA_ECDH1PU_P521_A256CBCHS512.fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(messages)) =>
        assertEquals(ks.keys.size, 9)

        ECPublicKey.decoder.decodeJson(JWKExamples.senderKeyP256_2)
        val signbyKey =
          JWKExamples.senderKeyP256_2.fromJson[ECPublicKey].toOption.get

        Future.sequence(messages.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decryptAndVerify(key.get, signbyKey, recipient.encrypted_key, messages).map {
            // {"payload":"eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19",
            //  "signatures":[
            //    {"protected":"eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRWREU0EifQ",
            //     "signature":"FW33NnvOHV0Ted9-F7GZbkia-vYAfBKtH4oBxbrttWAhBZ6UFJMxcGjL3lwOl4YohI3kyyd08LHPWNMgP2EVCQ",
            //     "header":{"kid":"did:example:alice#key-1"}
            //    }
            // ]}
            _.fromJson[SignedMessage] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, SignedMessageExample.exampleSignatureEdDSA_obj)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  // TODO encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P

  // TODO
  // test("Encrypt") {
  //   encrypt(JWKExamples.senderKeyP521.fromJson[PublicKey].toOption.get)
  // }

}
