package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.crypto.OKP_EC_Key
import java.util.Base64
import zio.json.ast.Json

/** DIDComm messaging
  *
  * The encrypted form of a JWM is a JWE in General JSON Format.
  *   - JWM - https://datatracker.ietf.org/doc/html/draft-looker-jwm-01
  *   - DIDComm encrypted message - https://identity.foundation/didcomm-messaging/spec/#didcomm-encrypted-messages
  *   - JWA - https://datatracker.ietf.org/doc/html/rfc7518
  *
  * DIDComm messaging support only a subset of the supported algorithms of JWAs
  *
  * Types:
  *   - "authcrypt" - Authenticated Sender Encryption
  *     - ECDH-1PU MUST be used within the structure of a JWE.
  *       https://datatracker.ietf.org/doc/html/draft-madden-jose-ecdh-1pu-04
  *   - "anoncrypt" - Anonymous Sender Encryption
  *     - ECDH-ES defined by RFC-7518 be used within the structure of a JWE.
  *       https://tools.ietf.org/html/rfc7518#section-4.6
  *
  * Content Encryption Algorithms: (with a AES 256-bit keys, or with an optional implementation using XC20P algorithm)
  * https://identity.foundation/didcomm-messaging/spec/#curves-and-content-encryption-algorithms
  *   - X25519
  *   - P-384
  *   - P-256 (deprecated in favor of P-384)
  *   - P-521 (Optional)
  *
  * JWE `protected` header `enc` MUST be one of:
  *   - A256CBC-HS512
  *   - A256GCM
  *   - XC20P (XChaCha20Poly1305 with a 256 bit key)
  *
  * JWE `protected` header `alg` can be one of:
  * https://identity.foundation/didcomm-messaging/spec/#key-wrapping-algorithms
  *   - ECDH-ES+A256KW (with P-256; P-384; P-521; X25519) for "anoncrypt" messages
  *   - ECDH-1PU+A256KW (with P-256; P-384; P-521; X25519) for "authcrypt" messages
  */
trait EncryptedMessage {
  def ciphertext: Base64URL
  def `protected`: Base64URLHeaders
  def recipients: Seq[Recipient]
  def tag: AuthenticationTag
  def iv: InitializationVector
}

// case class AnoncryptMessages //TODO
// case class AuthcryptMessages //TODO

/** TODO - To prevent invalid curve and weak point attacks, implementations that decrypt messages from a NIST curve MUST
  * verify that the received public key (contained in the JWE protected header) is on the curve in question. This check
  * may already be done by some JOSE libraries, but developers should not assume this is the case. See this explanation
  * of the risk, and this practical guide for how to perform the verification correctly.
  * https://neilmadden.blog/2017/05/17/so-how-do-you-validate-nist-ecdh-public-keys/
  */
case class EncryptedMessageGeneric(
    ciphertext: Base64URL,
    `protected`: Base64URLHeaders,
    recipients: Seq[Recipient],
    tag: AuthenticationTag,
    iv: InitializationVector
) extends EncryptedMessage {
  def headersAsJson = String(Base64.getDecoder().decode(`protected`)).fromJson[Json]
  // def headers: HeadersJson = protectedAsString.fromJson[HeadersJson] //TODO
}

object EncryptedMessageGeneric {
  given decoder: JsonDecoder[EncryptedMessageGeneric] = DeriveJsonDecoder.gen[EncryptedMessageGeneric]
  given encoder: JsonEncoder[EncryptedMessageGeneric] = DeriveJsonEncoder.gen[EncryptedMessageGeneric]
}

trait HeadersJson { // TODO
  def epk: Required[OKP_EC_Key]
  def skid: String

  /** base64URL(skid value) */
  def apu: Base64URL

  def apv: Base64URL

  def typ: String // like "application/didcomm-encrypted+json"
  def enc: String // like "XC20P"
  def alg: String // like "ECDH-ES+A256KW"
}

case class Recipient(
    encrypted_key: Base64URL,
    header: RecipientHeader,
)
object Recipient {
  given decoder: JsonDecoder[Recipient] = DeriveJsonDecoder.gen[Recipient]
  given encoder: JsonEncoder[Recipient] = DeriveJsonEncoder.gen[Recipient]
}

case class RecipientHeader(kid: VerificationMethodReferenced)
object RecipientHeader {
  given decoder: JsonDecoder[RecipientHeader] = DeriveJsonDecoder.gen[RecipientHeader]
  given encoder: JsonEncoder[RecipientHeader] = DeriveJsonEncoder.gen[RecipientHeader]
}
