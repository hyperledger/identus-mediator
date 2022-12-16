package fmgp.did.comm

import fmgp.did._
import fmgp.util.Base64
import zio.json._
import zio.json.ast.Json

// sealed trait Attachment {
//   def data: NotRequired[String]
//   def jws: NotRequired[String]
//   def hash: NotRequired[String]
//   def links: NotRequired[String]
//   def base64: NotRequired[String]
//   def json: NotRequired[String]
//   def byte_count: NotRequired[String]
// }

case class Attachment(
    id: NotRequired[String],
    /** A human-readable description of the content. */
    description: NotRequired[String],
    filename: NotRequired[String],
    media_type: NotRequired[String],
    format: NotRequired[String],
    lastmod_time: NotRequired[String],
    data: AttachmentData,
    /** Mostly relevant when content is included by reference instead of by value. Lets the receiver guess how expensive
      * it will be, in time, bandwidth, and storage, to fully fetch the attachment.
      */
    byte_count: NotRequired[String],
)
object Attachment {
  given decoder: JsonDecoder[Attachment] = DeriveJsonDecoder.gen[Attachment]
  given encoder: JsonEncoder[Attachment] = DeriveJsonEncoder.gen[Attachment]
}

//** https://www.rfc-editor.org/rfc/rfc7515#appendix-F */
type JWS_WithOutPayload = Json //TODO

sealed trait AttachmentData
case class AttachmentDataJWS(jws: JWS_WithOutPayload) extends AttachmentData
case class AttachmentDataLinks(links: String, hash: Required[String]) extends AttachmentData
case class AttachmentDataBase64(base64: Base64 /*, hash: NotRequired[String]*/ ) extends AttachmentData
case class AttachmentDataJson(json: Seq[Json] /*, hash: NotRequired[String]*/ ) extends AttachmentData

object AttachmentData {
  given decoder: JsonDecoder[AttachmentData] =
    AttachmentDataJWS.decoder.widen[AttachmentData] <>
      AttachmentDataLinks.decoder.widen[AttachmentData] <>
      AttachmentDataBase64.decoder.widen[AttachmentData] <>
      AttachmentDataJson.decoder.widen[AttachmentData]

  given encoder: JsonEncoder[AttachmentData] = new JsonEncoder[AttachmentData] {
    override def unsafeEncode(b: AttachmentData, indent: Option[Int], out: zio.json.internal.Write): Unit = b match
      case obj: AttachmentDataJWS    => AttachmentDataJWS.encoder.unsafeEncode(obj, indent, out)
      case obj: AttachmentDataLinks  => AttachmentDataLinks.encoder.unsafeEncode(obj, indent, out)
      case obj: AttachmentDataBase64 => AttachmentDataBase64.encoder.unsafeEncode(obj, indent, out)
      case obj: AttachmentDataJson   => AttachmentDataJson.encoder.unsafeEncode(obj, indent, out)
  }
}
object AttachmentDataJWS {
  given decoder: JsonDecoder[AttachmentDataJWS] = DeriveJsonDecoder.gen[AttachmentDataJWS]
  given encoder: JsonEncoder[AttachmentDataJWS] = DeriveJsonEncoder.gen[AttachmentDataJWS]
}
object AttachmentDataLinks {
  given decoder: JsonDecoder[AttachmentDataLinks] = DeriveJsonDecoder.gen[AttachmentDataLinks]
  given encoder: JsonEncoder[AttachmentDataLinks] = DeriveJsonEncoder.gen[AttachmentDataLinks]
}
object AttachmentDataBase64 {
  given decoder: JsonDecoder[AttachmentDataBase64] = DeriveJsonDecoder.gen[AttachmentDataBase64]
  given encoder: JsonEncoder[AttachmentDataBase64] = DeriveJsonEncoder.gen[AttachmentDataBase64]
}
object AttachmentDataJson {
  given decoder: JsonDecoder[AttachmentDataJson] = DeriveJsonDecoder.gen[AttachmentDataJson]
  given encoder: JsonEncoder[AttachmentDataJson] = DeriveJsonEncoder.gen[AttachmentDataJson]
}

/** A JSON object that gives access to the actual content of the attachment.
  *
  * This MUST contain at least one of the following subfields, and enough of them to allow access to the data:
  */
trait AttachmentDataAtLeastOne {

  /** OPTIONAL. A JWS in detached content mode, where the payload field of the JWS maps to base64 or to something
    * fetchable via links. This allows attachments to be signed. The signature need not come from the author of the
    * message.
    */
  def jws: Option[JWS_WithOutPayload]

  /** OPTIONAL. The hash of the content encoded in multi-hash format. Used as an integrity check for the attachment, and
    * MUST be used if the data is referenced via the links data attribute.
    */
  def hash: Option[String]

  /** OPTIONAL. A list of zero or more locations at which the content may be fetched. This allows content to be attached
    * by reference instead of by value.
    */
  def links: Option[String]

  /** OPTIONAL. Base64url-encoded data, when representing arbitrary content inline instead of via links. */
  def base64: Option[Base64]

  /** OPTIONAL. Directly embedded JSON data, when representing content inline instead of via links, and when the content
    * is natively conveyable as JSON.
    */
  def json: Option[Seq[Json]]
}
