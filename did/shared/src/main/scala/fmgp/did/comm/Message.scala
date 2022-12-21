package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.util._
import fmgp.crypto.OKP_EC_Key
import zio.json.ast.Json
import zio.json.ast.JsonCursor

/** DID Comm Message */
sealed trait Message

object Message {
  given decoder: JsonDecoder[Message] =
    PlaintextMessageClass.decoder.widen[Message] <>
      SignedMessage.decoder.widen[Message] <>
      EncryptedMessageGeneric.decoder.widen[Message]

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

  /** type or piuri is a URI that associates the body of a plaintext message with a published and versioned schema */
  def `type`: PIURI

  def to: NotRequired[Set[DIDSubject]]

  /** OPTIONAL when the message is to be encrypted via anoncrypt;
    *
    * TODO REQUIRED when the message is encrypted via authcrypt
    */
  def from: NotRequired[DIDSubject]

  /** Thread identifier */
  def thid: NotRequired[String]
  // TODO def pthid: NotRequired[String] // "1e513ad4-48c9-444e-9e7e-5b8b45c5e325",
  // TODO def ack: NotRequired[Seq[String]] // ["1e513ad4-48c9-444e-9e7e-5b8b45c5e325"],

  def created_time: NotRequired[UTCEpoch]
  def expires_time: NotRequired[UTCEpoch]

  /** application-level data inside a JSON */
  def body: Required[JSON_RFC7159]

  def attachments: NotRequired[Seq[Attachment]]
}

object PlaintextMessage {
  given decoder: JsonDecoder[PlaintextMessage] =
    PlaintextMessageClass.decoder.map(e => e: PlaintextMessage)

  given encoder: JsonEncoder[PlaintextMessage] = PlaintextMessageClass.encoder.contramap(
    _ match { case msg: PlaintextMessageClass => msg }
  )
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
    payload: Payload,
    signatures: Seq[JWMSignatureObj]
) extends Message {
  def base64 = signatures.head.`protected` + "." + payload + "." + signatures.head.signature
}

object SignedMessage {
  given decoder: JsonDecoder[SignedMessage] = DeriveJsonDecoder.gen[SignedMessage]
  given encoder: JsonEncoder[SignedMessage] = DeriveJsonEncoder.gen[SignedMessage]
}

case class JWMSignatureObj(`protected`: JWM_PROTECTED, signature: JWM_SIGNATURE, header: Option[JWMHeader] = None)
object JWMSignatureObj {
  given decoder: JsonDecoder[JWMSignatureObj] = DeriveJsonDecoder.gen[JWMSignatureObj]
  given encoder: JsonEncoder[JWMSignatureObj] = DeriveJsonEncoder.gen[JWMSignatureObj]
}

case class JWMHeader(kid: String) //FIXME DIDSuject
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
  def ciphertext: CipherText
  def `protected`: Base64Obj[ProtectedHeader]
  def recipients: Seq[Recipient]
  def tag: TAG
  def iv: IV
}

// trait AnonEncryptedMessage //TODO and make EncryptedMessage a sealed trait
// trait AuthEncryptedMessage //TODO and make EncryptedMessage a sealed trait

object EncryptedMessage {
  given decoder: JsonDecoder[EncryptedMessage] =
    EncryptedMessageGeneric.decoder.map(e => e: EncryptedMessage)

  given encoder: JsonEncoder[EncryptedMessage] = EncryptedMessageGeneric.encoder.contramap(
    _ match { case msg: EncryptedMessageGeneric => msg }
  )
}

extension (c: EncryptedMessage) {
  def skid = c.`protected`.obj match {
    case o: AuthProtectedHeader => Some(o.skid)
    case o: AnonProtectedHeader => None
  }
}

case class Recipient(
    encrypted_key: Base64,
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
