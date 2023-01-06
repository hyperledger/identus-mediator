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
    id: NotRequired[String] = None,
    /** A human-readable description of the content. */
    description: NotRequired[String] = None,
    filename: NotRequired[String] = None,
    media_type: NotRequired[String] = None,
    format: NotRequired[String] = None,
    lastmod_time: NotRequired[String] = None,
    data: AttachmentData,
    /** Mostly relevant when content is included by reference instead of by value. Lets the receiver guess how expensive
      * it will be, in time, bandwidth, and storage, to fully fetch the attachment.
      */
    byte_count: NotRequired[String] = None,
)
object Attachment {
  given decoder: JsonDecoder[Attachment] = DeriveJsonDecoder.gen[Attachment]
  given encoder: JsonEncoder[Attachment] = DeriveJsonEncoder.gen[Attachment]
}

//** https://www.rfc-editor.org/rfc/rfc7515#appendix-F */
type JWS_WithOutPayload = Json //TODO

/** A JSON object that gives access to the actual content of the attachment.
  *
  * This MUST contain at least one of the following subfields, and enough of them to allow access to the data:
  *
  * @param jws
  *   OPTIONAL. A JWS in detached content mode, where the payload field of the JWS maps to base64 or to something
  *   fetchable via links. This allows attachments to be signed. The signature need not come from the author of the
  *   message.
  * @param hash
  *   OPTIONAL. The hash of the content encoded in multi-hash format. Used as an integrity check for the attachment, and
  *   MUST be used if the data is referenced via the links data attribute.
  * @param links
  *   OPTIONAL. A list of zero or more locations at which the content may be fetched. This allows content to be attached
  *   by reference instead of by value.
  * @param base64
  *   OPTIONAL. Base64url-encoded data, when representing arbitrary content inline instead of via links.
  * @param json
  *   OPTIONAL. Directly embedded JSON data, when representing content inline instead of via links, and when the content
  *   is natively conveyable as JSON.
  */
sealed trait AttachmentData
case class AttachmentDataJWS(jws: JWS_WithOutPayload, links: Option[Seq[String]]) extends AttachmentData
case class AttachmentDataLinks(links: Seq[String], hash: Required[String]) extends AttachmentData
case class AttachmentDataBase64(base64: Base64) extends AttachmentData
case class AttachmentDataJson(json: Json) extends AttachmentData

/** This class is not intended to be used. (Is just a fallback to be fully compatible with the specification) */
case class AttachmentDataAny(
    jws: Option[JWS_WithOutPayload],
    hash: Option[String],
    links: Option[Seq[String]],
    base64: Option[Base64],
    json: Option[Json],
) extends AttachmentData

object AttachmentData {
  given decoder: JsonDecoder[AttachmentData] =
    AttachmentDataBase64.decoder.widen[AttachmentData] <>
      AttachmentDataJson.decoder.widen[AttachmentData] <>
      AttachmentDataJWS.decoder
        .mapOrFail { e => if (e.jws == Json.Null) Left("jws can not be null") else Right(e) } // TODO JWS_WithOutPayload
        .widen[AttachmentData] <>
      AttachmentDataLinks.decoder.widen[AttachmentData] <> // Note Try to decode Links after try JWS to work properly
      // TODO The code will not get here if we matches any other first!
      AttachmentDataAny.decoder.widen[AttachmentData] // Works as a fallback.

  given encoder: JsonEncoder[AttachmentData] = new JsonEncoder[AttachmentData] {
    override def unsafeEncode(b: AttachmentData, indent: Option[Int], out: zio.json.internal.Write): Unit = b match
      case obj: AttachmentDataJWS    => AttachmentDataJWS.encoder.unsafeEncode(obj, indent, out)
      case obj: AttachmentDataLinks  => AttachmentDataLinks.encoder.unsafeEncode(obj, indent, out)
      case obj: AttachmentDataBase64 => AttachmentDataBase64.encoder.unsafeEncode(obj, indent, out)
      case obj: AttachmentDataJson   => AttachmentDataJson.encoder.unsafeEncode(obj, indent, out)
      case obj: AttachmentDataAny    => AttachmentDataAny.encoder.unsafeEncode(obj, indent, out)
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
object AttachmentDataAny {
  given decoder: JsonDecoder[AttachmentDataAny] = DeriveJsonDecoder.gen[AttachmentDataAny]
  given encoder: JsonEncoder[AttachmentDataAny] = DeriveJsonEncoder.gen[AttachmentDataAny]
}
