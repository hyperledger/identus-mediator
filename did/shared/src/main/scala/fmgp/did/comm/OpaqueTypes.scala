package fmgp.did.comm

import zio.json._
import fmgp.util.Base64
import fmgp.crypto.SHA256
import fmgp.did.VerificationMethodReferenced

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

/** APV is a Base64 url encode */
opaque type APV = String
object APV:
  def apply(value: String): APV = value
  def apply(refs: Seq[VerificationMethodReferenced]): APV = {
    val data = SHA256.digest(refs.map(_.value).sorted.mkString(".").getBytes())
    Base64.encode(data).urlBase64
  }
  extension (apv: APV)
    def value: String = apv
    def base64: Base64 = Base64.fromBase64url(apv.value)

  given decoder: JsonDecoder[APV] = JsonDecoder.string.map(APV(_))
  given encoder: JsonEncoder[APV] = JsonEncoder.string.contramap[APV](_.value)

/** APU is a Base64 url encode */
opaque type APU = String
object APU:
  def apply(value: String): APU = value
  def apply(ref: VerificationMethodReferenced): APU =
    Base64.encode(ref.value.getBytes).urlBase64
  extension (apu: APU)
    def value: String = apu
    def base64: Base64 = Base64.fromBase64url(apu.value)

  given decoder: JsonDecoder[APU] = JsonDecoder.string.map(APU(_))
  given encoder: JsonEncoder[APU] = JsonEncoder.string.contramap[APU](_.value)

opaque type PIURI = String
object PIURI:
  def apply(value: String): PIURI = value
  extension (piuri: PIURI) def value: String = piuri
  given decoder: JsonDecoder[PIURI] = JsonDecoder.string.map(PIURI(_))
  given encoder: JsonEncoder[PIURI] = JsonEncoder.string.contramap[PIURI](_.value)

/** The Value is a short (<=32 bytes) string consisting entirely of unreserved URI characters.
  *
  * unreserved URI characters - https://datatracker.ietf.org/doc/html/rfc3986/#section-2.3:
  *   - Characters that are allowed in a URI but do not have a reserved purpose are called unreserved. These include
  *     uppercase and lowercase letters, decimal digits, hyphen, period, underscore, and tilde:
  *   - unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
  */
opaque type MsgID = String
object MsgID:
  /** @param value
    *   MUST be a maximum of 64 unreserved URI characters // TODO
    */
  def apply(value: String): MsgID = value
  def apply(): MsgID = java.util.UUID.randomUUID.toString() // Should be cross platform
  extension (messageID: MsgID) def value: String = messageID
  given decoder: JsonDecoder[MsgID] = JsonDecoder.string.map(MsgID(_))
  given encoder: JsonEncoder[MsgID] = JsonEncoder.string.contramap[MsgID](_.value)

opaque type Payload = Base64
object Payload:
  def fromBase64url(data: String): Payload = Base64.fromBase64url(data)
  extension (data: Payload)
    def content: String = data.decodeToString
    def base64url: String = data.urlBase64
  given decoder: JsonDecoder[Payload] = Base64.decoder
  given encoder: JsonEncoder[Payload] = Base64.encoder
