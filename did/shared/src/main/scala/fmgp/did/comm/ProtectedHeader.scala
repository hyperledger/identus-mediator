package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.crypto.PublicKey
import fmgp.util.Base64
import java.security.MessageDigest

// class Base64JWEHeader(data: Base64) extends Selectable:
//   val json = data.decode.fromJson[Json].toOption.get
//   def selectDynamic(name: String): Any = json.get(JsonCursor.field(name))

// type JsonJWEHeader = Base64JWEHeader {
//   val name: String
//   val age: Int
// }

/** {{{
  * "epk": {"kty":"OKP","crv":"X25519","x":"JHjsmIRZAaB0zRG_wNXLV2rPggF00hdHbW5rj8g0I24"},
  * "apv":"NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA",
  * "typ":"application/didcomm-encrypted+json",
  * "enc":"XC20P",
  * "alg":"ECDH-ES+A256KW"
  * }}}
  */
case class ProtectedHeader(
    epk: Option[PublicKey],
    apv: APV,
    skid: Option[VerificationMethodReferenced] = None, // did:example:alice#key-p256-1
    apu: Option[APU] = None,
    typ: MediaTypes,
    enc: ENCAlgorithm,
    alg: KWAlgorithm,
) {
  // Asserts for DEBUG
  if (typ == MediaTypes.ANONCRYPT | typ == MediaTypes.ANONCRYPT_AUTHCRYPT | typ == MediaTypes.ANONCRYPT_SIGN)
    assert(skid.isEmpty, "ANON messagem MUST NOT have 'skid'") // IMPROVE make it type safe
  if (typ == MediaTypes.AUTHCRYPT | typ == MediaTypes.AUTHCRYPT_SIGN)
    assert(skid.isDefined, "AUTH messagem MUST HAVE 'skid'") // IMPROVE make it type safe
}

object ProtectedHeader {
  given decoder: JsonDecoder[ProtectedHeader] = DeriveJsonDecoder.gen[ProtectedHeader]
  given encoder: JsonEncoder[ProtectedHeader] = DeriveJsonEncoder.gen[ProtectedHeader]
}

enum ENCAlgorithm { // JWAAlgorithm
  case `XC20P` extends ENCAlgorithm
  case `A256GCM` extends ENCAlgorithm
  case `A256CBC-HS512` extends ENCAlgorithm
}
object ENCAlgorithm {
  given decoder: JsonDecoder[ENCAlgorithm] = JsonDecoder.string.map(ENCAlgorithm.valueOf)
  given encoder: JsonEncoder[ENCAlgorithm] = JsonEncoder.string.contramap((e: ENCAlgorithm) => e.toString)
}

/** Key Wrapping Algorithms */
enum KWAlgorithm {
  case `ECDH-ES+A256KW` extends KWAlgorithm
  case `ECDH-1PU+A256KW` extends KWAlgorithm
}
object KWAlgorithm {
  given decoder: JsonDecoder[KWAlgorithm] = JsonDecoder.string.map(KWAlgorithm.valueOf)
  given encoder: JsonEncoder[KWAlgorithm] = JsonEncoder.string.contramap((e: KWAlgorithm) => e.toString)
}
