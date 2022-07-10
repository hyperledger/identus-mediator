package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.did.comm.PlaintextMessage.{UTCEpoch, JSON_RFC7159}

/** https://identity.foundation/didcomm-messaging/spec/#message-headers */
trait PlaintextMessage {

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
  type UTCEpoch = Long
  type JSON_RFC7159 = Map[String, String]
}

case class PlaintextMessageClass(
    id: Required[String],
    `type`: Required[String],
    to: NotRequired[Set[DIDURLSyntax]],
    from: NotRequired[DIDURLSyntax],
    thid: NotRequired[String],
    created_time: NotRequired[UTCEpoch],
    expires_time: NotRequired[UTCEpoch],
    body: Required[JSON_RFC7159],
    // FIXME attachments: NotRequired[Seq[Attachment]]
) extends PlaintextMessage

object PlaintextMessageClass {
  given decoder: JsonDecoder[PlaintextMessageClass] = DeriveJsonDecoder.gen[PlaintextMessageClass]
  given encoder: JsonEncoder[PlaintextMessageClass] = DeriveJsonEncoder.gen[PlaintextMessageClass]
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
