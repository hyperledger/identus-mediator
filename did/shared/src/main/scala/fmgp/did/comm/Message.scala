package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.crypto.OKP_EC_Key
import zio.json.ast.Json
import java.util.Base64
import zio.json.ast.JsonCursor

/** DID Comm Message */
sealed trait Message

object Message {
  given decoder: JsonDecoder[Message] =
    PlaintextMessageClass.decoder.map(e => e: Message) <>
      SignedMessage.decoder.map(e => e: Message) <>
      EncryptedMessageGeneric.decoder.map(e => e: Message)

  given encoder: JsonEncoder[Message] = new JsonEncoder[Message] {
    override def unsafeEncode(b: Message, indent: Option[Int], out: zio.json.internal.Write): Unit = {
      b match {
        case obj: PlaintextMessage => PlaintextMessage.encoder.unsafeEncode(obj, indent, out)
        case obj: SignedMessage    => SignedMessage.encoder.unsafeEncode(obj, indent, out)
        case obj: EncryptedMessage => EncryptedMessage.encoder.unsafeEncode(obj, indent, out)
      }
    }
  }
}

// ############################
// ##### PlaintextMessage #####
// ############################

/** https://identity.foundation/didcomm-messaging/spec/#message-headers */
trait PlaintextMessage extends Message {

  /** MUST be unique to the sender */
  def id: Required[String]

  /** A URI that associates the body of a plaintext message with a published and versioned schema */
  def `type`: Required[String]

  def to: NotRequired[Set[DIDURLSyntax]]

  /** OPTIONAL when the message is to be encrypted via anoncrypt;
    *
    * TODO REQUIRED when the message is encrypted via authcrypt
    */
  def from: NotRequired[DIDURLSyntax]

  /** Thread identifier */
  def thid: NotRequired[String]

  def created_time: NotRequired[UTCEpoch]
  def expires_time: NotRequired[UTCEpoch]

  /** application-level data inside a JSON */
  def body: Required[JSON_RFC7159]

  // FIXME def attachments: NotRequired[Seq[Attachment]]
}

object PlaintextMessage {
  given decoder: JsonDecoder[PlaintextMessage] =
    PlaintextMessageClass.decoder.map(e => e: PlaintextMessage)

  given encoder: JsonEncoder[PlaintextMessage] = PlaintextMessageClass.encoder.contramap(
    _ match { case msg: PlaintextMessageClass => msg }
  )
}

trait Attachment {
  def id: String

  /** A human-readable description of the content. */
  def description: String

  def filename: String
  def media_type: String
  def format: String
  def lastmod_time: String

  def data: AttachmentData

  /** Mostly relevant when content is included by reference instead of by value. Lets the receiver guess how expensive
    * it will be, in time, bandwidth, and storage, to fully fetch the attachment.
    */
  def byte_count: String
}

/** A JSON object that gives access to the actual content of the attachment.
  *
  * This MUST contain at least one of the following subfields, and enough of them to allow access to the data:
  */
trait AttachmentData {

  /** OPTIONAL. A JWS in detached content mode, where the payload field of the JWS maps to base64 or to something
    * fetchable via links. This allows attachments to be signed. The signature need not come from the author of the
    * message.
    */
  def jws: String

  /** OPTIONAL. The hash of the content encoded in multi-hash format. Used as an integrity check for the attachment, and
    * MUST be used if the data is referenced via the links data attribute.
    */
  def hash: String

  /** OPTIONAL. A list of zero or more locations at which the content may be fetched. This allows content to be attached
    * by reference instead of by value.
    */
  def links: String

  /** OPTIONAL. Base64url-encoded data, when representing arbitrary content inline instead of via links. */
  def base64: String

  /** OPTIONAL. Directly embedded JSON data, when representing content inline instead of via links, and when the content
    * is natively conveyable as JSON.
    */
  def json: String
}

enum ContentEncryptionAlgorithms {
// The underlying curve is actually Curve25519, however when used in the context of Diffie-Hellman the identifier of X25519 is used
  case X25519
// NIST defined P-384 elliptic curve
  case P384
// NIST defined P-256 elliptic curve - deprecated in favor of P-384
  case P256
// NIST defined P-521 elliptic curve. Optional.
  case P521
}

// #########################
// ##### SignedMessage #####
// #########################

/** SignedMessage is a JSON Web Message (JWM)
  *
  * JWM is a flexible way to encode application-level messages in JSON for transfer over a variety of transport
  * protocols. JWMs use JSON Web Encryption (JWE) to protect integrity, achieve confidentiality, and achieve repudiable
  * authentication; alternatively or in addition, they use JSON Web Signatures (JWS) to associate messages with a
  * non-repudiable digital signature.
  *
  * https://datatracker.ietf.org/doc/html/draft-looker-jwm-01
  */
case class SignedMessage(
    payload: Base64URL,
    signatures: Seq[JWMSignatureObj]
) extends Message {
  def base64 = signatures.head.`protected` + "." + payload + "." + signatures.head.signature
}

object SignedMessage {
  given decoder: JsonDecoder[SignedMessage] = DeriveJsonDecoder.gen[SignedMessage]
  given encoder: JsonEncoder[SignedMessage] = DeriveJsonEncoder.gen[SignedMessage]
}

case class JWMSignatureObj(`protected`: String, signature: String, header: Option[JWMHeader] = None)
object JWMSignatureObj {
  given decoder: JsonDecoder[JWMSignatureObj] = DeriveJsonDecoder.gen[JWMSignatureObj]
  given encoder: JsonEncoder[JWMSignatureObj] = DeriveJsonEncoder.gen[JWMSignatureObj]
}

case class JWMHeader(kid: String)
object JWMHeader {
  given decoder: JsonDecoder[JWMHeader] = DeriveJsonDecoder.gen[JWMHeader]
  given encoder: JsonEncoder[JWMHeader] = DeriveJsonEncoder.gen[JWMHeader]
}

// ############################
// ##### EncryptedMessage #####
// ############################

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
trait EncryptedMessage extends Message {
  def ciphertext: Base64URL
  def `protected`: Base64URLHeaders
  def recipients: Seq[Recipient]
  def tag: AuthenticationTag
  def iv: InitializationVector
}

object EncryptedMessage {
  given decoder: JsonDecoder[EncryptedMessage] =
    EncryptedMessageGeneric.decoder.map(e => e: EncryptedMessage)

  given encoder: JsonEncoder[EncryptedMessage] = EncryptedMessageGeneric.encoder.contramap(
    _ match { case msg: EncryptedMessageGeneric => msg }
  )
}

extension (c: EncryptedMessage) {
  def headersAsJson = String(Base64.getDecoder().decode(c.`protected`)).fromJson[Json]
  def skid = c.headersAsJson
    .flatMap(_.get(JsonCursor.field("skid")))
    .flatMap(_.as[String])
  // def headers: HeadersJson = protectedAsString.fromJson[HeadersJson] //TODO
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
