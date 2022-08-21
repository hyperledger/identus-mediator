package fmgp.did.comm

import munit._
import zio.json._
import fmgp.did.DIDDocument
import fmgp.crypto._

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import fmgp.did.VerificationMethodReferenced
import java.util.Base64
import zio.json.ast.JsonCursor

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
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, message).map {
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
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, message).map {
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
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, message).map {
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
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)

        val signbyKey = JWKExamples.senderKeyX25519
          .fromJson[OKPPublicKey]
          .toOption
          .get

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decryptAndVerify(key.get, signbyKey, recipient.encrypted_key, message).map {
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
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)

        ECPublicKey.decoder.decodeJson(JWKExamples.senderKeyP256_2)
        val signbyKey =
          JWKExamples.senderKeyP256_2.fromJson[ECPublicKey].toOption.get

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decryptAndVerify(key.get, signbyKey, recipient.encrypted_key, message).map {
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

  test("decrypt encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P
        .fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)
        Future
          .sequence(message.recipients.toSeq.map { recipient =>
            val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
            assert(key.isDefined)
            decrypt(key.get, recipient.encrypted_key, message).map {
              _.fromJson[EncryptedMessageGeneric] match {
                case Left(error) => fail(error)
                case Right(obj)  =>
                  // {"epk":{"kty":"EC","crv":"P-521","x":"ASvgMsQUnY_bj9aYhpm-pS4YU6pQ2BQh3quiBKQJkoIQpIkTsMu-E2EsZyoNHwWj4fhyyOkoL_4v-P3joigCIYAl","y":"AbJmO50e2ccsdvhewqhVLY9tZckh7PHKqoi0y6leNMOTzhfh9aCTOXl7Vk1WzNDsZ1sFWmDwNtrDRfmH142SWxyv"},
                  //  "apv":"GOeo76ym6NCg9WWMEYfW0eVDT5668zEhl2uAIW-E-HE"
                  //  "skid":"did:example:alice#key-p521-1",
                  //  "apu":"ZGlkOmV4YW1wbGU6YWxpY2Uja2V5LXA1MjEtMQ",
                  //  "typ":"application/didcomm-encrypted+json",
                  //  "enc":"A256CBC-HS512",
                  //  "alg":"ECDH-1PU+A256KW"}
                  Future.sequence(obj.recipients.toSeq.map { recipient2 =>
                    val key2 = ks.keys.find(e => e.kid.contains(recipient2.header.kid.value))
                    val signbyKey2 = JWKExamples.senderKeyP521.fromJson[ECPublicKey].toOption.get
                    decryptAndVerify(key2.get, signbyKey2, recipient2.encrypted_key, obj)
                  })
              }
            }.flatten
          })
          .map(_.flatten)
          .map(_.map { str =>
            str.fromJson[SignedMessage] match {
              case Left(error) => fail(error)
              case Right(obj) =>
                val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
                key.verify(obj).map(e => assert(e))
                assertEquals(obj, SignedMessageExample.exampleSignatureEdDSA_obj)
            }
          })

      case data => fail(data.toString)
    }

  }

  // ###############
  // ### encrypt ###
  // ###############

  val example2encrypt = PlaintextMessageClass(
    "987654321",
    "https://app.fmgp/protocols/chat/1.0/message",
    Some(Set("did:example:bob")),
    Some("did:example:alice"),
    None,
    Some(1516269022),
    Some(1516385931),
    Map("text" -> "Hey Bob")
  )

  test("encrypt with ECDHES_X25519_A256CBCHS512".tag(fmgp.JsUnsupported)) { // FIXME ECDHES_X25519_XC20P
    DIDCommExamples.recipientSecrets.fromJson[KeyStore] match {
      case (Right(ks)) =>
        assertEquals(ks.keys.size, 9)
        val data = example2encrypt.toJson.getBytes

        val kidKeys = Seq(
          "did:example:bob#key-x25519-1",
          "did:example:bob#key-x25519-2",
          "did:example:bob#key-x25519-3",
        ).map { kid =>
          val key = ks.keys.find(e => e.kid.contains(kid)).get
          (VerificationMethodReferenced(kid), key.toPublicKey)
        }

        val message = encrypt(kidKeys, data)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apv")).isRight)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apu")).isLeft)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("skid")).isLeft)
        assert(message.recipients.size == 3)

        Future
          .sequence(message.recipients.map { recipient =>
            val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
            assert(key.isDefined)

            decrypt(key.get, recipient.encrypted_key, message).map {
              _.fromJson[PlaintextMessageClass] match {
                case Left(error) => fail(error)
                case Right(obj) =>
                  assertEquals(obj, example2encrypt)
              }
            }
          })
      case data => fail(data.toString)
    }
  }

  test("encrypt with ECDHES_P384_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    DIDCommExamples.recipientSecrets.fromJson[KeyStore] match {
      case Right(ks) =>
        assertEquals(ks.keys.size, 9)
        val data = example2encrypt.toJson.getBytes

        val kidKeys = Seq(
          "did:example:bob#key-p384-1",
          "did:example:bob#key-p384-2",
        ).map { kid =>
          val key = ks.keys.find(e => e.kid.contains(kid)).get
          (VerificationMethodReferenced(kid), key.toPublicKey)
        }

        val message = encrypt(kidKeys, data)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apv")).isRight)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apu")).isLeft)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("skid")).isLeft)
        assert(message.recipients.size == 2)

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, message).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, example2encrypt)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  test("encrypt with ECDHES_P521_A256CBCHS512".tag(fmgp.JsUnsupported)) { // FIXME ECDHES_P521_A256GCM
    DIDCommExamples.recipientSecrets.fromJson[KeyStore] match {
      case Right(ks) =>
        assertEquals(ks.keys.size, 9)
        val data = example2encrypt.toJson.getBytes

        val kidKeys = Seq(
          "did:example:bob#key-p521-1",
          "did:example:bob#key-p521-2",
        ).map { kid =>
          val key = ks.keys.find(e => e.kid.contains(kid)).get
          (VerificationMethodReferenced(kid), key.toPublicKey)
        }

        val message = encrypt(kidKeys, data)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apv")).isRight)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apu")).isLeft)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("skid")).isLeft)
        assert(message.recipients.size == 2)

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)
          decrypt(key.get, recipient.encrypted_key, message).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, example2encrypt)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  test("encrypt with ECDH1PU_X25519_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      JWKExamples.senderKeyX25519.fromJson[OKPPrivateKey]
    ) match {
      case (Right(ks), Right(senderKey)) =>
        assertEquals(ks.keys.size, 9)
        val data = example2encrypt.toJson.getBytes

        val message = encrypt(
          (VerificationMethodReferenced("did:example:alice#key-x25519-1"), senderKey),
          Seq(
            "did:example:bob#key-x25519-1",
            "did:example:bob#key-x25519-2",
            "did:example:bob#key-x25519-3",
          ).map(kid => (VerificationMethodReferenced(kid), ks.keys.find(e => e.kid.contains(kid)).get.toPublicKey)),
          data
        )
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apv")).isRight)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apu")).isRight)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("skid")).isRight)
        assert(message.recipients.size == 3)

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)

          decryptAndVerify(key.get, senderKey.toPublicKey, recipient.encrypted_key, message).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, example2encrypt)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  test("encrypt with EdDSA_ECDH1PU_P521_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      JWKExamples.senderKeyP256_2.fromJson[ECPrivateKey]
    ) match {
      case (Right(ks), Right(senderKey)) =>
        assertEquals(ks.keys.size, 9)
        val data = example2encrypt.toJson.getBytes

        val message = encrypt(
          (VerificationMethodReferenced("did:example:alice#key-p256-1"), senderKey),
          Seq(
            "did:example:bob#key-p256-1",
            "did:example:bob#key-p256-2",
          ).map(kid => (VerificationMethodReferenced(kid), ks.keys.find(e => e.kid.contains(kid)).get.toPublicKey)),
          data
        )

        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apv")).isRight)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("apu")).isRight)
        assert(message.headersAsJson.toOption.get.get(JsonCursor.field("skid")).isRight)
        assert(message.recipients.size == 2)

        Future.sequence(message.recipients.map { recipient =>
          val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
          assert(key.isDefined)

          decryptAndVerify(key.get, senderKey.toPublicKey, recipient.encrypted_key, message).map {
            _.fromJson[PlaintextMessageClass] match {
              case Left(error) => fail(error)
              case Right(obj)  => assertEquals(obj, example2encrypt)
            }
          }
        })
      case data => fail(data.toString)
    }
  }

  // TODO  encrypt with EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P

}
