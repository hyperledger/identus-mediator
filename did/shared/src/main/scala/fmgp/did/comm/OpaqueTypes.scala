package fmgp.did.comm

import zio.json._
import fmgp.util.Base64

/** CipherText is a Base64 url encode */
opaque type CipherText = String
object CipherText:
  def apply(value: String): CipherText = value
  extension (ciphertext: CipherText)
    def value: String = ciphertext
    def base64: Base64 = Base64.fromBase64url(ciphertext.value)
  given decoder: JsonDecoder[CipherText] = JsonDecoder.string.map(CipherText(_))
  given encoder: JsonEncoder[CipherText] = JsonEncoder.string.contramap[CipherText](_.value)

/** InitializationVector is a Base64 url encode */
opaque type IV = String
object IV:
  def apply(value: String): IV = value
  extension (iv: IV)
    def value: String = iv
    def base64: Base64 = Base64.fromBase64url(iv.value)
  given decoder: JsonDecoder[IV] = JsonDecoder.string.map(IV(_))
  given encoder: JsonEncoder[IV] = JsonEncoder.string.contramap[IV](_.value)

/** AuthenticationTag is a Base64 url encode */
opaque type TAG = String
object TAG:
  def apply(value: String): TAG = value
  extension (tag: TAG)
    def value: String = tag
    def base64: Base64 = Base64.fromBase64url(tag.value)
  given decoder: JsonDecoder[TAG] = JsonDecoder.string.map(TAG(_))
  given encoder: JsonEncoder[TAG] = JsonEncoder.string.contramap[TAG](_.value)
