/*
package fmgp.crypto

import typings.jose.anon.PickFlattenedJWEencrypted
import typings.jose.errorsMod.JWEDecryptionFailed
import typings.jose.typesMod.JWEHeaderParameters
import typings.jose.typesMod.GeneralJWE
import typings.jose.typesMod.DecryptOptions
import typings.jose.mod.generalDecrypt

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON
import scala.scalajs.js.JavaScriptException
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._
import concurrent.ExecutionContext.Implicits.global

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.UtilsJS._
import fmgp.crypto.error._

object RawOperations extends CryptoOperations {

  override def sign(key: PrivateKey, plaintext: PlaintextMessage): IO[CryptoFailed, SignedMessage] =
    PlatformSpecificOperations.sign(key, plaintext)

  override def verify(key: PublicKey, jwm: SignedMessage): IO[CryptoFailed, Boolean] =
    PlatformSpecificOperations.verify(key, jwm)

  // ###############
  // ### encrypt ###
  // ###############
  // TODO

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

  override def anonEncrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = ZIO.die(NotImplementedError()) // FIXME

  override def authEncrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = ZIO.die(NotImplementedError()) // FIXME

  // ###############
  // ### decrypt ###
  // ###############

  override def anonDecrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = ZIO.die(NotImplementedError()) // FIXME

  override def authDecrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = ZIO.die(NotImplementedError()) // FIXME

  /** See https://github.com/panva/jose/blob/HEAD/docs/functions/jwe_general_decrypt.generalDecrypt.md#readme */
  def anonDecryptOne(
      key: PrivateKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = {
    val aux = msg.recipients
      .find(_.encrypted_key.urlBase64 == encryptedKey)
      .map { r =>
        PickFlattenedJWEencrypted()
          .setEncrypted_key(r.encrypted_key.urlBase64)
          .setHeader {
            val h = JWEHeaderParameters()
            h.kid = r.header.kid.value
            h
          }
      }
      .get
    val jweJS = GeneralJWE(
      msg.ciphertext.value,
      msg.iv.value,
      js.Array(aux), // recipients: js.Array[PickFlattenedJWEencrypted],
      msg.tag.value,
    )
      .setProtected(msg.`protected`.base64url)

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

    key.toKeyLike
      .flatMap { (thisKey, alg) =>
        ZIO
          .fromPromiseJS(generalDecrypt(jweJS, thisKey, decryptOptions))
          .mapError {
            case eee @ scala.scalajs.js.JavaScriptException(ex) => // if (ex.isInstanceOf[JWEDecryptionFailed]) =>
              println("$" * 100 + " " + ex.asInstanceOf[JWEDecryptionFailed].stack) // FIXME println
              DecryptionFailed
          }
          .map { ret => String(ret.plaintext.toArray.map(e => e.toByte)) }
      }
      .flatMap { str =>
        ZIO.fromEither(str.fromJson[Message].left.map(FailToParse(_)))
      }
  }

  def authDecryptOne(
      recipientKey: PrivateKey,
      senderKey: PublicKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = ZIO.die(NotImplementedError()) // FIXME

}
 */
