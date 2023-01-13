package fmgp.util

import java.{util => ju}
import zio.json._

// Base64 URL
opaque type Base64 = Array[Byte]
//opaque type Base64 = Seq[Byte]

object Base64:
  /** Base64 url encoder RFC4648 */
  val urlEncoder = ju.Base64.getUrlEncoder

  /** Base64 url decoder RFC4648 */
  val urlDecoder = ju.Base64.getUrlDecoder

  /** Base64 basic encoder RFC4648 */
  val basicEncoder = ju.Base64.getEncoder

  /** Base64 basic decoder RFC4648 */
  val basicDecoder = ju.Base64.getDecoder

  given decoder: JsonDecoder[Base64] = JsonDecoder.string.map(Base64.apply _)
  given encoder: JsonEncoder[Base64] = JsonEncoder.string.contramap[Base64](e => e.urlBase64)

  /** @param str
    *   Base64 URL string
    *
    * TODO: method copyArrayToImmutableIndexedSeq in class LowPriorityImplicits2 is deprecated since 2.13.0: Implicit
    * conversions from Array to immutable.IndexedSeq are implemented by copying; Use the more efficient non-copying
    * ArraySeq.unsafeWrapArray or an explicit toIndexedSeq call
    */
  def apply(str: String): Base64 = str.getBytes

  /** @param str
    *   Base64 URL Byte
    */
  def apply(bytes: Array[Byte]): Base64 = bytes
  def fromBase64url(str: String): Base64 = str.getBytes
  def fromBase64(str: String): Base64 = encode(basicDecoder.decode(str.getBytes))
  def encode(str: String): Base64 = urlEncoder.encode(str.getBytes)
  def encode(data: Array[Byte]): Base64 = urlEncoder.encode(data)

  extension (bytes: Base64)
    def bytes: Array[Byte] = bytes
    def urlBase64: String = String(bytes).filterNot(_ == '=')
    def basicBase64: String = String(Base64.basicEncoder.encode(Base64.urlDecoder.decode(bytes)))
    def decode: Array[Byte] = Base64.urlDecoder.decode(bytes)
    def decodeToString: String = String(Base64.urlDecoder.decode(bytes))

    /** Decodes this Base64 object to an unsigned big integer. */
    def decodeToBigInt = BigInt(1, bytes.decode)
    def decodeToHex = bytes.decode.map("%02X".format(_)).mkString

/** Base64Obj keep the original base64 encoder (useful to preserve data for doing MAC checks) */
case class Base64Obj[T](obj: T, original: Option[Base64] = None) {
  def base64url(using jsonEncoder: JsonEncoder[T]): String = Base64.encode(obj.toJson).urlBase64
}
object Base64Obj {
  given decoder[T](using jsonDecoder: JsonDecoder[T]): JsonDecoder[Base64Obj[T]] =
    Base64.decoder.mapOrFail { original =>
      original.decodeToString
        .fromJson[T]
        .map(
          Base64Obj(_, Some(original)) // Store the original
        )
    }

  given encoder[T](using jsonEncoder: JsonEncoder[T]): JsonEncoder[Base64Obj[T]] =
    Base64.encoder.contramap[Base64Obj[T]] {
      case Base64Obj(_, Some(original)) => original
      case Base64Obj(obj, None)         => Base64.encode(obj.toJson)
    }
}
