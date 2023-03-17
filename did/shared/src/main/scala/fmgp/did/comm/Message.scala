package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.did.comm.extension._
import fmgp.util._
import fmgp.crypto.OKP_EC_Key
import zio.json.ast.Json
import zio.json.ast.JsonCursor

import fmgp.did.comm.ReturnRoute

/** DID Comm Message */
sealed trait Message

object Message {
  given decoder: JsonDecoder[Message] =
    //   PlaintextMessageClass.decoder.widen[Message] <>
    //     SignedMessage.decoder.widen[Message] <>
    //     EncryptedMessageGeneric.decoder.widen[Message]
    PlaintextMessageClass.decoder
      .widen[Message]
      .orElseAndWarpErrors(SignedMessage.decoder.widen[Message])
      .orElseAndWarpErrors(EncryptedMessageGeneric.decoder.widen[Message])

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
  def id: MsgID

  /** type or piuri is a URI that associates the body of a plaintext message with a published and versioned schema */
  def `type`: PIURI

  def to: NotRequired[Set[TO]]

  /** OPTIONAL when the message is to be encrypted via anoncrypt;
    *
    * TODO REQUIRED when the message is encrypted via authcrypt
    */
  def from: NotRequired[FROM]

  /** Thread identifier */
  def thid: NotRequired[MsgID]
  // TODO def pthid: NotRequired[String] // "1e513ad4-48c9-444e-9e7e-5b8b45c5e325",
  // TODO def ack: NotRequired[Seq[String]] // ["1e513ad4-48c9-444e-9e7e-5b8b45c5e325"],

  def created_time: NotRequired[UTCEpoch]
  def expires_time: NotRequired[UTCEpoch]

  /** application-level data inside a JSON
    *
    * This attribute MUST be present, even if empty. It MUST be a JSON object conforming to RFC 7159.
    */
  def body: Required[JSON_RFC7159]

  def attachments: NotRequired[Seq[Attachment]]

  /** DIDComm defines a specific header to handle DID rotation. This header is called from_prior and can be used in any
    * message sent to the other party. That message must include the from_prior header that is a standard JWT token
    * conformed with:
    *   - Header:
    *     - typ: jwt
    *     - alg: verification algorithm such as EdDSA
    *     - crv: curve name
    *     - kid: key id from previous DID that is used in the signature of this JWT
    *   - Payload:
    *     - sub: the new DID
    *     - iss: the previous DID
    *     - iat: datetime in seconds
    *   - Signature: from the previous DID and key defined in the kid
    *
    * For more information @see https://didcomm.org/book/v2/didrotation
    */
  def from_prior: NotRequired[JWTToken]

  /** Extension Return Route Header
    *
    * For HTTP transports, the presence of this message decorator indicates that the receiving agent MAY hold onto the
    * connection and use it to return messages as designated. HTTP transports will only be able to receive at most one
    * message at a time. Websocket transports are capable of receiving multiple messages over a single connection.
    *
    * @see
    *   https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/return_route/main.md
    *
    *   - none: Default. No messages should be returned over this connection. If return_route is omitted, this is the
    *     default value.
    *   - all: Send all messages for this DID over the connection.
    *   - thread: Send all messages matching the DID and thread specified in the return_route_thread attribute.
    *     - TODO what is return_route_thread?
    */
  def return_route: NotRequired[ReturnRoute]

  // Extension: https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/l10n/main.md
  def `accept-lang`: NotRequired[Seq[LanguageCodeIANA]]
  def lang: NotRequired[LanguageCodeIANA] // IANA’s language codes  // IANA’s language subtag registry.
  def l10n: NotRequired[L10n]

  // Extension: https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/advanced_sequencing/main.md
  def sender_order: NotRequired[SenderOrder]
  def sent_count: NotRequired[SentCount]
  def received_orders: NotRequired[Seq[ReceivedOrdersElement]]
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
  def base64 = signatures.head.`protected` + "." + payload.base64url + "." + signatures.head.signature
  def base64noSignature = signatures.head.`protected` + "." + payload.base64url
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

case class JWMHeader(kid: String) //FIXME DIDURI
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

  // extra
  def recipientsSubject = recipients.map(_.recipientSubject).toSet
  def recipientsKid = recipients.map(_.recipientKid).toSet
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
) {
  def recipientSubject: DIDSubject = header.didSubject
  def recipientKid: VerificationMethodReferenced = header.kid
}
object Recipient {
  given decoder: JsonDecoder[Recipient] = DeriveJsonDecoder.gen[Recipient]
  given encoder: JsonEncoder[Recipient] = DeriveJsonEncoder.gen[Recipient]
}

case class RecipientHeader(kid: VerificationMethodReferenced) {
  def didSubject = kid.did
}
object RecipientHeader {
  given decoder: JsonDecoder[RecipientHeader] = DeriveJsonDecoder.gen[RecipientHeader]
  given encoder: JsonEncoder[RecipientHeader] = DeriveJsonEncoder.gen[RecipientHeader]
}
