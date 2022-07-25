package fmgp

import fmgp.did.comm._
import typings.jose._
import typings.jose.anon.PickFlattenedJWEencrypted
import typings.jose.decodeJwtMod
import typings.jose.errorsMod.JWEDecryptionFailed
import typings.jose.joseBooleans
import typings.jose.joseRequire
import typings.jose.joseStrings
import typings.jose.mod.CompactSign
import typings.jose.mod.EncryptJWT
import typings.jose.mod.GeneralEncrypt
import typings.jose.mod.GeneralSign
import typings.jose.mod.SignJWT
import typings.jose.mod.compactDecrypt
import typings.jose.mod.exportJWK
import typings.jose.mod.generalDecrypt
import typings.jose.mod.generateKeyPair
import typings.jose.mod.importJWK
import typings.jose.mod.jwtDecrypt
import typings.jose.mod.jwtVerify
import typings.jose.typesMod.CompactJWEHeaderParameters
import typings.jose.typesMod.CompactJWSHeaderParameters
import typings.jose.typesMod.DecryptOptions
import typings.jose.typesMod.GeneralJWE
import typings.jose.typesMod.GeneralJWSInput
import typings.jose.typesMod.JWEHeaderParameters
import typings.jose.typesMod.JWK
import typings.jose.typesMod.JWTPayload
import typings.jose.typesMod.KeyLike
import typings.jose.typesMod.ResolvedKey
import typings.std.CryptoKey
import typings.std.global.TextEncoder
import zio.json._

import scala.concurrent.Future
import scala.concurrent.Future.apply
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON
import scala.scalajs.js.JavaScriptException
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._

import concurrent.ExecutionContext.Implicits.global

/** See https://www.npmjs.com/package/jose */
package object crypto {

  // extension (ec: ECKey) {

  //   // https://datatracker.ietf.org/doc/html/draft-ietf-jose-json-web-key
  //   def toJWKECKey = {
  //     val key = JWK()
  //     key.setKty(ec.kty.toString)
  //     key.setX(ec.x)
  //     key.setY(ec.y)
  //     key.setCrv(ec.getCurve.toString)
  //     // TODO REMOVE
  //     // ec.getCurve match { // See table in https://github.com/panva/jose/issues/210
  //     //   case Curve.`P-256`   => key.setAlg("ES256") // JWKECKey.apply(ECCurveJS.`P-256`, ec.x, ec.y)
  //     //   case Curve.`P-384`   => key.setAlg("ES384") // JWKECKey.apply(ECCurveJS.`P-384`, ec.x, ec.y)
  //     //   case Curve.`P-521`   => key.setAlg("ES512") // JWKECKey.apply(ECCurveJS.`P-521`, ec.x, ec.y)
  //     //   case Curve.secp256k1 => key.setAlg("ES256K") // JWKECKey.apply(ECCurveJS.secp256k1, ec.x, ec.y)
  //     // }
  //     key.setAlg(ec.alg)
  //     ec.kid.foreach(id => key.setKid(id))

  //     ec match {
  //       case _: PublicKey  => // ok
  //       case k: PrivateKey => key.setD(k.d)
  //     }
  //     key
  //   }
  // }

  // extension (okp: OKPKey) {
  //   def toJWKOKPKey = {
  //     val key = JWK()
  //     key.setKty(okp.kty.toString)
  //     key.setX(okp.x)
  //     key.setCrv(okp.getCurve.toString)
  //     // TODO REMOVE
  //     // okp.getCurve match {
  //     //   case Curve.Ed25519 => key.setAlg("EdDSA") // JWKOKPKey.apply(OKPCurveJS.Ed25519, okp.x)
  //     //   case Curve.X25519  => key.setAlg("EdDSA") // FIXME CHECK // JWKOKPKey.apply(OKPCurveJS.X25519, okp.x)
  //     // }
  //     key.setAlg(okp.alg)
  //     okp.kid.foreach(id => key.setKid(id))
  //     okp match {
  //       case _: PublicKey  => // ok
  //       case k: PrivateKey => key.setD(k.d)
  //     }
  //     key
  //   }
  // }

  extension (key: OKP_EC_Key) {

    private def toJWK: JWK = {
      val keyJWK = JWK()
      keyJWK.setKty(key.kty.toString)
      keyJWK.setX(key.x)
      key match {
        case ec: ECKey   => keyJWK.setY(ec.y)
        case okp: OKPKey => // ok
      }

      keyJWK.setCrv(key.crv.toString)
      keyJWK.setAlg(key.alg.toString)
      key.kid.foreach(id => keyJWK.setKid(id))

      key match {
        case _: PublicKey  => // ok
        case k: PrivateKey => keyJWK.setD(k.d)
      }
      keyJWK
    }

    private def toKeyLike: Future[(KeyLike, String)] = {
      val aux = key.toJWK
      importJWK(aux).toFuture.map(k => (k.asInstanceOf[KeyLike], aux.alg.get))
    }

    def verify(jwm: SignedMessage): Future[Boolean] =
      key.toKeyLike.flatMap(thisKey =>
        jwtVerify(jwm.base64, thisKey._1).toFuture
          .map(_ => true)
          .recover { case JavaScriptException(ex: scala.scalajs.js.TypeError) => false }
      )
  }

  extension (key: PrivateKey) {
    def sign(plaintext: PlaintextMessageClass): Future[SignedMessage] = { // TODO use PlaintextMessage
      val data = js.typedarray.Uint8Array.from(plaintext.toJson.map(_.toShort).toJSIterable)
      key.toKeyLike
        .flatMap { (thisKey, alg) =>
          GeneralSign(data) // We can also use CompactSign
            .tap(
              _.addSignature(thisKey.asInstanceOf[KeyLike])
                .setProtectedHeader(CompactJWSHeaderParameters(alg))
            )
            .sign()
            .toFuture
            .map(generalJWS =>
              // TODO REMOVE old .split('.') match { case Array(protectedValue, payload, signature) =>
              SignedMessage(
                payload = generalJWS.payload,
                generalJWS.signatures.toSeq
                  .map(v => JWMSignatureObj(`protected` = v.`protected`.get, signature = v.signature))
              )
            )

        }

    }
  }

  // ###############
  // ### encrypt ###
  // ###############
  // TODO

  def encrypt(recipientKey: PublicKey) = ???
  // {
  //   recipientKey.toKeyLike
  //     .map(_._1)
  //     .flatMap { recipientKeyLike =>
  //       val data = new TextEncoder().encode("It’s a dangerous business, Frodo, going out your door.")

  //       // "enc":"A256CBC-HS512","alg":"ECDH-ES+A256KW"
  //       // "enc":"XC20P","alg":"ECDH-ES+A256KW"}

  //       val aux = GeneralEncrypt(data)
  //       // aux.setProtectedHeader(JWEHeaderParameters().setEnc("A256CBC-HS512").setAlg("ECDH-ES+A256KW"))
  //       aux.setProtectedHeader(JWEHeaderParameters().setEnc("XC20P").setAlg("ECDH-ES+XC20PKW"))
  //       aux.addRecipient(recipientKeyLike)
  //       val tmp = aux.encrypt()
  //       tmp.toFuture
  //     }
  //     .map { e =>
  //       println("%" * 100)
  //       println(JSON.stringify(e))
  //       e
  //     }
  // }

  // ###############
  // ### decrypt ###
  // ###############

  /** See https://github.com/panva/jose/blob/HEAD/docs/functions/jwe_general_decrypt.generalDecrypt.md#readme
    */
  def decrypt(
      key: PrivateKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): Future[String] = {
    val aux = msg.recipients
      .find(_.encrypted_key == encryptedKey)
      .map { r =>
        PickFlattenedJWEencrypted()
          .setEncrypted_key(r.encrypted_key)
          .setHeader {
            val h = JWEHeaderParameters()
            h.kid = r.header.kid.value
            h
          }
      }
      .get
    val jweJS = GeneralJWE(
      msg.ciphertext, // ciphertext: String,
      msg.iv, // iv: String,
      js.Array(aux), // recipients: js.Array[PickFlattenedJWEencrypted],
      msg.tag, // tag: String
    )
      .setProtected(msg.`protected`)

    // "enc":"A256CBC-HS512","alg":"ECDH-ES+A256KW"
    // "enc":"XC20P","alg":"ECDH-ES+A256KW"}
    val decryptOptions = DecryptOptions()
    decryptOptions
      .setContentEncryptionAlgorithms(
        // A list of accepted JWE "enc" (Encryption Algorithm) Header Parameter values. By default all "enc" (Encryption Algorithm) values applicable for the used key/secret are allowed.
        // https://www.rfc-editor.org/rfc/rfc7518#section-5.1
        js.Array(
          // "A128CBC-HS256",
          // "A192CBC-HS384",
          "A256CBC-HS512", // Required for Authcrypt/Anoncrypt
          // "A128GCM",
          // "A192GCM",
          "A256GCM", // Recommended for Anoncrypt
          // FIXME "XC20P", // Optional for Anoncrypt see https://github.com/panva/jose/discussions/237 and also https://hackmd.io/@IyhpRay4QVC_ozugDsQAQg/S1QlYJN0d
        )
      )
      .setKeyManagementAlgorithms(
        // A list of accepted JWE "alg" (Algorithm) Header Parameter values.
        // https://www.rfc-editor.org/rfc/rfc7518#section-4.1
        js.Array(
          // "RSA1_5",
          // "RSA-OAEP",
          // "RSA-OAEP-256",
          // "A128KW",
          // "A192KW",
          // "A256KW",
          // "dir",
          // "ECDH-ES",
          // "ECDH-ES+A128KW",
          // "ECDH-ES+A192KW",
          "ECDH-ES+A256KW", // use
          // "A128GCMKW",
          // "A192GCMKW",
          // "A256GCMKW",
          // "PBES2-HS256+A128KW",
          // "PBES2-HS384+A192KW",
          // "PBES2-HS512+A256KW",
          // FIXME need ECDH-1PU+A256KW // Not supported
        )
      )

    key.toKeyLike.flatMap { (thisKey, alg) =>
      generalDecrypt(jweJS, thisKey, decryptOptions).toFuture
        .recover {
          case eee @ scala.scalajs.js.JavaScriptException(ex) => // if (ex.isInstanceOf[JWEDecryptionFailed]) =>
            println("$" * 100 + " " + ex.asInstanceOf[JWEDecryptionFailed].stack)
            throw eee
        }
        .map { ret => String(ret.plaintext.toArray.map(e => e.toByte)) }
    }
  }

  def decryptAndVerify(
      keyDecrypt: PrivateKey,
      keyToVerify: PublicKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): Future[String] = ???

}
