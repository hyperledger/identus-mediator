package fmgp.did.comm

import fmgp.crypto.JWKExamples
import fmgp.did.DIDSubject
import zio.json.ast.Json

object DIDCommExamples {

  /** A DIDComm message in its plaintext form, not packaged into any protective envelope, is known as a DIDComm
    * plaintext message.
    */
  val dcpmLinkSVG = "https://identity.foundation/didcomm-messaging/collateral/dcpm.svg"

  /** A DIDComm signed message is a signed JWM envelope that associates a non-repudiable signature with the plaintext
    * message inside it.
    */
  val dcsmLinkSVG = "https://identity.foundation/didcomm-messaging/collateral/dcsm.svg"

  /** A DIDComm encrypted message is an encrypted JWM. It hides its content from all but authorized recipients,
    * discloses and proves the sender to exactly and only those recipients, and provides integrity guarantees. It is
    * important in privacy-preserving routing. It is what normally moves over network transports in DIDComm Messaging
    * applications, and is the safest format for storing DIDComm Messaging data at rest.
    */
  val dcemLinkSVG = "https://identity.foundation/didcomm-messaging/collateral/dcem.svg"

  val plaintextMessage = """
  {
  "id": "1234567890",
  "type": "<message-type-uri>",
  "from": "did:example:alice",
  "to": ["did:example:bob"],
  "created_time": 1516269022,
  "expires_time": 1516385931,
  "body": {
    "message_type_specific_attribute": "and its value",
    "another_attribute": "and its value"
  }
}
""".stripMargin
  val plaintextMessageObj = PlaintextMessageClass(
    id = MsgID("1234567890"),
    `type` = PIURI("<message-type-uri>"),
    from = Some(FROM("did:example:alice")),
    to = Some(Set(TO("did:example:bob"))),
    thid = None,
    created_time = Some(1516269022L),
    expires_time = Some(1516385931L),
    body = Some(
      Json.Obj(
        "message_type_specific_attribute" -> Json.Str("and its value"),
        "another_attribute" -> Json.Str("and its value")
      )
    ),
    attachments = None
  )

  def attachment = """
{
  "type": "<sometype>",
  "to": [
    "did:example:mediator"
  ],
  "body": {
    "attachment_id": "1",
    "encrypted_details": {
      "id": "x",
      "encrypted_to": "",
      "other_details": "about attachment"
    }
  },
  "attachments": [
    {
      "id": "1",
      "description": "example b64 encoded attachment",
      "data": {
        "base64": "WW91ciBob3ZlcmNyYWZ0IGlzIGZ1bGwgb2YgZWVscw=="
      }
    },
    {
      "id": "2",
      "description": "example linked attachment",
      "data": {
        "hash": "<multi-hash>",
        "links": [
          "https://path/to/resource"
        ]
      }
    },
    {
      "id": "x",
      "description": "example encrypted DIDComm message as attachment",
      "media_type": "application/didcomm-encrypted+json",
      "data": {
        "json": {
          //jwe json structure
        }
      }
    }
  ]
}
""".stripMargin

  // ####################################################################
  // ### https://identity.foundation/didcomm-messaging/spec/#appendix ###
  // ####################################################################

  val senderSecrets = s"""[
    |${JWKExamples.senderKeyEd25519},
    |${JWKExamples.senderKeyP256},
    |${JWKExamples.senderKeySecp256k1},
    |${JWKExamples.senderKeyX25519},
    |${JWKExamples.senderKeyP256_2},
    |${JWKExamples.senderKeyP521}]""".stripMargin

  val recipientSecrets = s"""[
    |${JWKExamples.recipientKey1},
    |${JWKExamples.recipientKey2},
    |${JWKExamples.recipientKey3},
    |${JWKExamples.recipientKey4},
    |${JWKExamples.recipientKey5},
    |${JWKExamples.recipientKey6},
    |${JWKExamples.recipientKey7},
    |${JWKExamples.recipientKey8},
    |${JWKExamples.recipientKey9}]""".stripMargin

  val senderDIDDocument = """{
   "@context":[
      "https://www.w3.org/ns/did/v1",
      "https://w3id.org/security/suites/jws-2020/v1"
   ],
   "id":"did:example:alice",
   "authentication":[
      {
         "id":"did:example:alice#key-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:alice#key-1",
         "publicKeyJwk":{
            "kty":"OKP",
            "crv":"Ed25519",
            "x":"G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww"
         }
      },
      {
         "id":"did:example:alice#key-2",
         "type":"JsonWebKey2020",
         "controller":"did:example:alice#key-2",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-256",
            "x":"2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY",
            "y":"BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w"
         }
      },
      {
         "id":"did:example:alice#key-3",
         "type":"JsonWebKey2020",
         "controller":"did:example:alice#key-3",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"secp256k1",
            "x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
            "y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
         }
      }
   ],
   "keyAgreement":[
      {
         "id":"did:example:alice#key-x25519-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:alice#key-x25519-1",
         "publicKeyJwk":{
            "kty":"OKP",
            "crv":"X25519",
            "x":"avH0O2Y4tqLAq8y9zpianr8ajii5m4F_mICrzNlatXs"
         }
      },
      {
         "id":"did:example:alice#key-p256-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:alice#key-p256-1",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-256",
            "x":"L0crjMN1g0Ih4sYAJ_nGoHUck2cloltUpUVQDhF2nHE",
            "y":"SxYgE7CmEJYi7IDhgK5jI4ZiajO8jPRZDldVhqFpYoo"
         }
      },
      {
         "id":"did:example:alice#key-p521-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:alice#key-p521-1",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-521",
            "x":"AHBEVPRhAv-WHDEvxVM9S0px9WxxwHL641Pemgk9sDdxvli9VpKCBdra5gg_4kupBDhz__AlaBgKOC_15J2Byptz",
            "y":"AciGcHJCD_yMikQvlmqpkBbVqqbg93mMVcgvXBYAQPP-u9AF7adybwZrNfHWCKAQwGF9ugd0Zhg7mLMEszIONFRk"
         }
      }
   ]
}""".stripMargin

  val recipientDIDDocument = """{
   "@context":[
      "https://www.w3.org/ns/did/v2"
   ],
   "id":"did:example:bob",
   "keyAgreement":[
      {
         "id":"did:example:bob#key-x25519-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-x25519-1",
         "publicKeyJwk":{
            "kty":"OKP",
            "crv":"X25519",
            "x":"GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
         }
      },
      {
         "id":"did:example:bob#key-x25519-2",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-x25519-2",
         "publicKeyJwk":{
            "kty":"OKP",
            "crv":"X25519",
            "x":"UT9S3F5ep16KSNBBShU2wh3qSfqYjlasZimn0mB8_VM"
         }
      },
      {
         "id":"did:example:bob#key-x25519-3",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-x25519-3",
         "publicKeyJwk":{
            "kty":"OKP",
            "crv":"X25519",
            "x":"82k2BTUiywKv49fKLZa-WwDi8RBf0tB0M8bvSAUQ3yY"
         }
      },
      {
         "id":"did:example:bob#key-p256-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-p256-1",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-256",
            "x":"FQVaTOksf-XsCUrt4J1L2UGvtWaDwpboVlqbKBY2AIo",
            "y":"6XFB9PYo7dyC5ViJSO9uXNYkxTJWn0d_mqJ__ZYhcNY"
         }
      },
      {
         "id":"did:example:bob#key-p256-2",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-p256-2",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-256",
            "x":"n0yBsGrwGZup9ywKhzD4KoORGicilzIUyfcXb1CSwe0",
            "y":"ov0buZJ8GHzV128jmCw1CaFbajZoFFmiJDbMrceCXIw"
         }
      },
      {
         "id":"did:example:bob#key-p384-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-p384-1",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-384",
            "x":"MvnE_OwKoTcJVfHyTX-DLSRhhNwlu5LNoQ5UWD9Jmgtdxp_kpjsMuTTBnxg5RF_Y",
            "y":"X_3HJBcKFQEG35PZbEOBn8u9_z8V1F9V1Kv-Vh0aSzmH-y9aOuDJUE3D4Hvmi5l7"
         }
      },
      {
         "id":"did:example:bob#key-p384-2",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-p384-2",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-384",
            "x":"2x3HOTvR8e-Tu6U4UqMd1wUWsNXMD0RgIunZTMcZsS-zWOwDgsrhYVHmv3k_DjV3",
            "y":"W9LLaBjlWYcXUxOf6ECSfcXKaC3-K9z4hCoP0PS87Q_4ExMgIwxVCXUEB6nf0GDd"
         }
      },
      {
         "id":"did:example:bob#key-p521-1",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-p521-1",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-521",
            "x":"Af9O5THFENlqQbh2Ehipt1Yf4gAd9RCa3QzPktfcgUIFADMc4kAaYVViTaDOuvVS2vMS1KZe0D5kXedSXPQ3QbHi",
            "y":"ATZVigRQ7UdGsQ9j-omyff6JIeeUv3CBWYsZ0l6x3C_SYqhqVV7dEG-TafCCNiIxs8qeUiXQ8cHWVclqkH4Lo1qH"
         }
      },
      {
         "id":"did:example:bob#key-p521-2",
         "type":"JsonWebKey2020",
         "controller":"did:example:bob#key-p521-2",
         "publicKeyJwk":{
            "kty":"EC",
            "crv":"P-521",
            "x":"ATp_WxCfIK_SriBoStmA0QrJc2pUR1djpen0VdpmogtnKxJbitiPq-HJXYXDKriXfVnkrl2i952MsIOMfD2j0Ots",
            "y":"AEJipR0Dc-aBZYDqN51SKHYSWs9hM58SmRY1MxgXANgZrPaq1EeGMGOjkbLMEJtBThdjXhkS5VlXMkF0cYhZELiH"
         }
      }
   ]
}""".stripMargin
}
