package fmgp.did.comm

import zio.json._
import zio.json.ast._

import fmgp.did._
import fmgp.crypto.PublicKey
import fmgp.util.Base64

// class Base64JWEHeader(data: Base64) extends Selectable:
//   val json = data.decode.fromJson[Json].toOption.get
//   def selectDynamic(name: String): Any = json.get(JsonCursor.field(name))

// type JsonJWEHeader = Base64JWEHeader {
//   val name: String
//   val age: Int
// }

sealed trait ProtectedHeaderTMP {
  // def epk: Option[PublicKey]
  def apv: APV
  // def skid: Option[VerificationMethodReferenced] = None
  // def apu: Option[APU] = None
  def typ: MediaTypes
  def enc: ENCAlgorithm
  def alg: KWAlgorithm
}

// FIXME replace ProtectedHeader
sealed trait ProtectedHeaderAUX extends ProtectedHeaderTMP {
  def epk: PublicKey
  def apv: APV
  // def skid: Option[VerificationMethodReferenced]
  // def apu: Option[APU]
  def typ: MediaTypes
  def enc: ENCAlgorithm
  def alg: KWAlgorithm
}

object ProtectedHeaderAUX {
  given decoder: JsonDecoder[ProtectedHeaderAUX] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst.get(JsonCursor.field("skid")) match {
      case Left(value) /* "No such field: 'skid' */ => AnonProtectedHeader.decoder.decodeJson(originalAst.toJson)
      case Right(value)                             => AuthProtectedHeader.decoder.decodeJson(originalAst.toJson)
    }
  }
  given encoder: JsonEncoder[ProtectedHeaderAUX] = new JsonEncoder[ProtectedHeaderAUX] {
    override def unsafeEncode(b: ProtectedHeaderAUX, indent: Option[Int], out: zio.json.internal.Write): Unit =
      b match {
        case obj: AnonProtectedHeader => AnonProtectedHeader.encoder.unsafeEncode(obj, indent, out)
        case obj: AuthProtectedHeader => AuthProtectedHeader.encoder.unsafeEncode(obj, indent, out)
      }
  }
}

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

case class AnonProtectedHeader(
    epk: PublicKey,
    apv: APV,
    typ: MediaTypes = MediaTypes.ANONCRYPT,
    enc: ENCAlgorithm,
    alg: KWAlgorithm,
) extends ProtectedHeaderAUX

object AnonProtectedHeader {
  given decoder: JsonDecoder[AnonProtectedHeader] = {
    given aux: JsonDecoder[AnonProtectedHeader] = DeriveJsonDecoder.gen[AnonProtectedHeader]
    Json.Obj.decoder.mapOrFail { originalAst =>
      originalAst.get(JsonCursor.field("skid")) match {
        case Left(value) /* "No such field: 'skid' */ => aux.decodeJson(originalAst.toJson)
        case Right(value)                             => Left("Found field 'skid'")
      }
    }
  }

  given encoder: JsonEncoder[AnonProtectedHeader] = DeriveJsonEncoder.gen[AnonProtectedHeader]
}

case class AuthProtectedHeader(
    epk: PublicKey,
    apv: APV,
    skid: VerificationMethodReferenced, // did:example:alice#key-p256-1
    apu: APU,
    typ: MediaTypes = MediaTypes.AUTHCRYPT,
    enc: ENCAlgorithm,
    alg: KWAlgorithm,
) extends ProtectedHeaderAUX

object AuthProtectedHeader {
  given decoder: JsonDecoder[AuthProtectedHeader] = DeriveJsonDecoder.gen[AuthProtectedHeader]
  given encoder: JsonEncoder[AuthProtectedHeader] = DeriveJsonEncoder.gen[AuthProtectedHeader]
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
