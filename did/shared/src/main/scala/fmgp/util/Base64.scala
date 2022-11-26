package fmgp.util

import java.{util => ju}
import zio.json._

// Base64 URL
opaque type Base64 = Array[Byte]

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
    def urlBase64: String = String(bytes).filterNot(_ == '=')
    def basicBase64: String = String(
      Base64.basicEncoder.encode(
        Base64.urlDecoder.decode(bytes)
      )
    )
    def bytes: Array[Byte] = bytes
    def decode: String = String(Base64.urlDecoder.decode(bytes))

case class Base64Obj[T](obj: T) {
  def base64url(using jsonEncoder: JsonEncoder[T]): String = Base64.encode(obj.toJson).urlBase64
}
object Base64Obj {
  given decoder[T](using jsonDecoder: JsonDecoder[T]): JsonDecoder[Base64Obj[T]] =
    Base64.decoder.mapOrFail(e => e.decode.fromJson[T].map(Base64Obj(_)))

  given encoder[T](using jsonEncoder: JsonEncoder[T]): JsonEncoder[Base64Obj[T]] =
    Base64.encoder.contramap[Base64Obj[T]](e => Base64.encode(e.obj.toJson))
}
