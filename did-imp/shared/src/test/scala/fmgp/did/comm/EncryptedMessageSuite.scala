package fmgp.did.comm

import munit._

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.RawOperations._

import zio._
import zio.json._
import zio.json.ast.JsonCursor
import zio.json.ast.Json

/* didImpJVM/testOnly fmgp.did.comm.EncryptedMessageSuite */
class EncryptedMessageSuite extends ZSuite {

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

  def test_anonDecrypt(msg: String) =
    (DIDCommExamples.recipientSecrets.fromJson[KeyStore], msg.fromJson[EncryptedMessageGeneric]) match {
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)

        val recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)] =
          message.recipients.map { recipient =>
            val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
            assert(key.isDefined)
            (VerificationMethodReferenced(recipient.header.kid.value), key.get)
          }

        anonDecrypt(recipientKidsKeys, message).map(msg => assertEquals(msg, expeted))
      case data => ZIO.dieMessage(data.toString)
    }

  testZ("decrypt encryptedMessage_ECDHES_X25519_XC20P".tag(fmgp.JsUnsupported)) {
    test_anonDecrypt(EncryptedMessageExamples.encryptedMessage_ECDHES_X25519_XC20P)
  }

  testZ("decrypt encryptedMessage_ECDHES_P384_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    test_anonDecrypt(EncryptedMessageExamples.encryptedMessage_ECDHES_P384_A256CBCHS512)
  }

  testZ("decrypt encryptedMessage_ECDHES_P521_A256GCM".tag(fmgp.JsUnsupported)) {
    test_anonDecrypt(EncryptedMessageExamples.encryptedMessage_ECDHES_P521_A256GCM)
  }

  testZ("decrypt encryptedMessage_ECDH1PU_X25519_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_ECDH1PU_X25519_A256CBCHS512
        .fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)
        val senderKey = JWKExamples.senderKeyX25519.fromJson[OKPPublicKey].toOption.get

        val recipientKidsKeys = message.recipients.map { r =>
          val vmr = r.header.kid
          val key = ks.keys.find(e => e.kid.contains(vmr.value)).get // FIXME get
          (vmr, key)
        }

        for {
          data <- authDecrypt(senderKey, recipientKidsKeys, message) // .orDie
          // obj <- ZIO.fromEither(data.fromJson[PlaintextMessageClass]) // .orDieWith(str => RuntimeException(str))
        } yield assertEquals(data, expeted)
      case data => ZIO.dieMessage(data.toString)
    }
  }

  testZ("decrypt encryptedMessage_EdDSA_ECDH1PU_P521_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_EdDSA_ECDH1PU_P521_A256CBCHS512.fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)

        val recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)] =
          message.recipients.map { recipient =>
            val key = ks.keys.find(e => e.kid.contains(recipient.header.kid.value))
            assert(key.isDefined)
            (VerificationMethodReferenced(recipient.header.kid.value), key.get)
          }

        val senderKey = JWKExamples.senderKeyP256_2.fromJson[ECPublicKey].toOption.get

        authDecrypt(senderKey, recipientKidsKeys, message)
          .flatMap {
            case msg: SignedMessage =>
              // {"payload":"eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19",
              //  "signatures":[
              //    {"protected":"eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRWREU0EifQ",
              //     "signature":"FW33NnvOHV0Ted9-F7GZbkia-vYAfBKtH4oBxbrttWAhBZ6UFJMxcGjL3lwOl4YohI3kyyd08LHPWNMgP2EVCQ",
              //     "header":{"kid":"did:example:alice#key-1"}
              //    }
              // ]}

              val signbyKey = JWKExamples.senderKeyEd25519.fromJson[OKPPublicKey].toOption.get
              verify(signbyKey, msg).map { b =>
                assert(b)
                assertEquals(msg, SignedMessageExample.exampleSignatureEdDSA_obj)
              }
            case msg => ZIO.fail(s"msg is not of the type SignedMessage: $msg")
          }

      case data => ZIO.dieMessage(data.toString)
    }
  }

  testZ("decrypt encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      EncryptedMessageExamples.encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P
        .fromJson[EncryptedMessageGeneric]
    ) match {
      case (Right(ks), Right(message)) =>
        assertEquals(ks.keys.size, 9)
        for {
          _ <- ZIO.unit
          // {"epk":{"kty":"EC","crv":"P-521",
          //        "x":"AblhyUD5LX4OsX8FE5Z84A_OBa8b8waUHWHQ1m0gs8nHUDD7rH9IEdYo2TK1PaNakNZJNxkQAX-ZRLVkPh6uxy2M",
          //        "y":"APN8zsLDdbiV3t-9hY2EC1Uea3Nnm31m5j0FcbQc4cfVBaMw5BCeiqOPZIce5L628nuNFLJGk3J8zIPOaIKSLfi1"},
          //  "apv":"GOeo76ym6NCg9WWMEYfW0eVDT5668zEhl2uAIW-E-HE",
          //  "typ":"application/didcomm-encrypted+json",
          //  "enc":"XC20P",
          //  "alg":"ECDH-ES+A256KW"}
          recipientKidsKeys1 = message.recipients.map { recipient =>
            ks.keys
              .find(_.kid.contains(recipient.header.kid.value))
              .map(key => (recipient.header.kid, key))
          }.flatten
          data2 <- anonDecrypt(recipientKidsKeys1, message)
          // message2 <- ZIO.fromEither(data2.fromJson[EncryptedMessageGeneric])
          message2 = {
            assert(data2.isInstanceOf[EncryptedMessageGeneric])
            data2.asInstanceOf[EncryptedMessageGeneric]
          }
          // {"epk":{"kty":"EC","crv":"P-521",
          //         "x":"ASvgMsQUnY_bj9aYhpm-pS4YU6pQ2BQh3quiBKQJkoIQpIkTsMu-E2EsZyoNHwWj4fhyyOkoL_4v-P3joigCIYAl",
          //         "y":"AbJmO50e2ccsdvhewqhVLY9tZckh7PHKqoi0y6leNMOTzhfh9aCTOXl7Vk1WzNDsZ1sFWmDwNtrDRfmH142SWxyv"},
          //  "apv":"GOeo76ym6NCg9WWMEYfW0eVDT5668zEhl2uAIW-E-HE",
          //  "skid":"did:example:alice#key-p521-1",
          //  "apu":"ZGlkOmV4YW1wbGU6YWxpY2Uja2V5LXA1MjEtMQ",
          //  "typ":"application/didcomm-encrypted+json",
          //  "enc":"A256CBC-HS512",
          //  "alg":"ECDH-1PU+A256KW"}
          recipientKidsKeys2 = message.recipients.map { recipient =>
            ks.keys
              .find(_.kid.contains(recipient.header.kid.value))
              .map(key => (recipient.header.kid, key))
          }.flatten
          signbyKey = JWKExamples.senderKeyP521.fromJson[ECPublicKey].toOption.get
          data3 <- authDecrypt(signbyKey, recipientKidsKeys2, message2)
          // message3 <- ZIO.fromEither(data3.fromJson[SignedMessage])
          message3 = {
            assert(data3.isInstanceOf[SignedMessage])
            data3.asInstanceOf[SignedMessage]
          }
        } yield assertEquals(message3, SignedMessageExample.exampleSignatureEdDSA_obj)
      case data => ZIO.dieMessage(data.toString)
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

  testZ("encrypt with ECDHES_X25519_A256CBCHS512".tag(fmgp.JsUnsupported)) { // FIXME ECDHES_X25519_XC20P
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

        for {
          message <- encrypt(kidKeys, data)
          _ = assert(!message.`protected`.obj.apv.base64.bytes.isEmpty) // REMOVE TEST
          _ = assert(message.`protected`.obj.apu.isEmpty)
          _ = assert(message.`protected`.obj.skid.isEmpty)
          _ = assert(message.recipients.size == 3)

          recipientKidsKeys = message.recipients.map { recipient =>
            ks.keys
              .find(_.kid.contains(recipient.header.kid.value))
              .map(key => (recipient.header.kid, key))
          }.flatten
          data <- anonDecrypt(recipientKidsKeys, message)
          // obj <- ZIO.fromEither(data.fromJson[PlaintextMessageClass])
        } yield assertEquals(data, example2encrypt)
      case data => fail(data.toString)
    }
  }

  testZ("encrypt with ECDHES_P384_A256CBCHS512".tag(fmgp.JsUnsupported)) {
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

        for {
          message <- encrypt(kidKeys, data)
          _ = assert(!message.`protected`.obj.apv.base64.bytes.isEmpty) // REMOVE TEST
          _ = assert(message.`protected`.obj.apu.isEmpty)
          _ = assert(message.`protected`.obj.skid.isEmpty)
          _ = assert(message.recipients.size == 2)

          recipientKidsKeys = message.recipients.map { recipient =>
            ks.keys
              .find(_.kid.contains(recipient.header.kid.value))
              .map(key => (recipient.header.kid, key))
          }.flatten
          data <- anonDecrypt(recipientKidsKeys, message)
          // obj <- ZIO.fromEither(data.fromJson[PlaintextMessageClass])
        } yield assertEquals(data, example2encrypt)

      case data => fail(data.toString)
    }
  }

  testZ("encrypt with ECDHES_P521_A256CBCHS512".tag(fmgp.JsUnsupported)) { // FIXME ECDHES_P521_A256GCM
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

        for {
          message <- encrypt(kidKeys, data)
          _ = assert(!message.`protected`.obj.apv.base64.bytes.isEmpty) // REMOVE TEST
          _ = assert(message.`protected`.obj.apu.isEmpty)
          _ = assert(message.`protected`.obj.skid.isEmpty)
          _ = assert(message.recipients.size == 2)

          recipientKidsKeys = message.recipients.map { recipient =>
            ks.keys
              .find(_.kid.contains(recipient.header.kid.value))
              .map(key => (recipient.header.kid, key))
          }.flatten
          data <- anonDecrypt(recipientKidsKeys, message)
          // obj <- ZIO.fromEither(data.fromJson[PlaintextMessageClass])
        } yield assertEquals(data, example2encrypt)

      case data => fail(data.toString)
    }
  }

  testZ("encrypt with ECDH1PU_X25519_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      JWKExamples.senderKeyX25519.fromJson[OKPPrivateKey]
    ) match {
      case (Right(ks), Right(senderKey)) =>
        assertEquals(ks.keys.size, 9)
        val data = example2encrypt.toJson.getBytes

        val senderKidKey = (VerificationMethodReferenced("did:example:alice#key-x25519-1"), senderKey)

        val kidKeys = Seq(
          "did:example:bob#key-x25519-1",
          "did:example:bob#key-x25519-2",
          "did:example:bob#key-x25519-3",
        ).map { kid =>
          val key = ks.keys.find(e => e.kid.contains(kid)).get
          (VerificationMethodReferenced(kid), key.toPublicKey)
        }

        for {
          message <- authEncrypt(senderKidKey, kidKeys, data)
          _ = assert(!message.`protected`.obj.apv.base64.bytes.isEmpty) // REMOVE TEST
          _ = assert(message.`protected`.obj.apu.isDefined)
          _ = assert(message.`protected`.obj.skid.isDefined)
          _ = assert(message.recipients.size == 3)

          recipientKidsKeys = message.recipients.map { recipient =>
            ks.keys
              .find(_.kid.contains(recipient.header.kid.value))
              .map(key => (recipient.header.kid, key))
          }.flatten
          data <- authDecrypt(senderKey.toPublicKey, recipientKidsKeys, message)
          // obj <- ZIO.fromEither(data.fromJson[PlaintextMessageClass])
        } yield assertEquals(data, example2encrypt)

      case data => fail(data.toString)
    }
  }

  testZ("encrypt with EdDSA_ECDH1PU_P521_A256CBCHS512".tag(fmgp.JsUnsupported)) {
    (
      DIDCommExamples.recipientSecrets.fromJson[KeyStore],
      JWKExamples.senderKeyP256_2.fromJson[ECPrivateKey]
    ) match {
      case (Right(ks), Right(senderKey)) =>
        assertEquals(ks.keys.size, 9)
        val data = example2encrypt.toJson.getBytes

        val senderKidKey = (VerificationMethodReferenced("did:example:alice#key-x25519-1"), senderKey)

        val kidKeys = Seq(
          "did:example:bob#key-p256-1",
          "did:example:bob#key-p256-2",
        ).map { kid =>
          val key = ks.keys.find(e => e.kid.contains(kid)).get
          (VerificationMethodReferenced(kid), key.toPublicKey)
        }

        for {
          message <- authEncrypt(senderKidKey, kidKeys, data)
          _ = assert(!message.`protected`.obj.apv.base64.bytes.isEmpty) // REMOVE TEST
          _ = assert(message.`protected`.obj.apu.isDefined)
          _ = assert(message.`protected`.obj.skid.isDefined)
          _ = assert(message.recipients.size == 2)

          recipientKidsKeys = message.recipients.map { recipient =>
            ks.keys
              .find(_.kid.contains(recipient.header.kid.value))
              .map(key => (recipient.header.kid, key))
          }.flatten
          data <- authDecrypt(senderKey.toPublicKey, recipientKidsKeys, message)
          // obj <- ZIO.fromEither(data.fromJson[PlaintextMessageClass])
        } yield assertEquals(data, example2encrypt)

      case data => fail(data.toString)
    }
  }

  // TODO  encrypt with EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P

}
