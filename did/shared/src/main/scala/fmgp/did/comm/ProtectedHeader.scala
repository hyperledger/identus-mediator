package fmgp.did.comm

import zio.json._
import zio.json.ast._

import fmgp.did._
import fmgp.crypto.PublicKey
import fmgp.util.{Base64, safeValueOf}

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

case class AnonHeaderBuilder(
    apv: APV,
    enc: ENCAlgorithm,
    alg: KWAlgorithm,
) extends ProtectedHeaderTMP {
  def typ: MediaTypes = MediaTypes.ANONCRYPT
  def buildWithKey(epk: PublicKey) = AnonProtectedHeader(epk, apv, typ, enc, alg)
}

case class AuthHeaderBuilder(
    apv: APV,
    skid: VerificationMethodReferenced,
    apu: APU,
    enc: ENCAlgorithm,
    alg: KWAlgorithm,
) extends ProtectedHeaderTMP {
  def typ: MediaTypes = MediaTypes.AUTHCRYPT
  def buildWithKey(epk: PublicKey) = AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg)
}

/** {{{
  * "epk": {"kty":"OKP","crv":"X25519","x":"JHjsmIRZAaB0zRG_wNXLV2rPggF00hdHbW5rj8g0I24"},
  * "apv":"NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA",
  * "typ":"application/didcomm-encrypted+json",
  * "enc":"XC20P",
  * "alg":"ECDH-ES+A256KW"
  * }}}
  */
sealed trait ProtectedHeader extends ProtectedHeaderTMP {
  def epk: PublicKey
  def apv: APV
  // def skid: Option[VerificationMethodReferenced]
  // def apu: Option[APU]
  def typ: MediaTypes
  def enc: ENCAlgorithm
  def alg: KWAlgorithm
}

object ProtectedHeader {
  given decoder: JsonDecoder[ProtectedHeader] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst.get(JsonCursor.field("skid")) match {
      case Left(value) /* "No such field: 'skid' */ => AnonProtectedHeader.decoder.decodeJson(originalAst.toJson)
      case Right(value)                             => AuthProtectedHeader.decoder.decodeJson(originalAst.toJson)
    }
  }
  given encoder: JsonEncoder[ProtectedHeader] = new JsonEncoder[ProtectedHeader] {
    override def unsafeEncode(b: ProtectedHeader, indent: Option[Int], out: zio.json.internal.Write): Unit =
      b match {
        case obj: AnonProtectedHeader => AnonProtectedHeader.encoder.unsafeEncode(obj, indent, out)
        case obj: AuthProtectedHeader => AuthProtectedHeader.encoder.unsafeEncode(obj, indent, out)
      }
  }
}

case class AnonProtectedHeader(
    epk: PublicKey,
    apv: APV,
    typ: MediaTypes = MediaTypes.ANONCRYPT,
    enc: ENCAlgorithm,
    alg: KWAlgorithm,
) extends ProtectedHeader

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
) extends ProtectedHeader

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
  given decoder: JsonDecoder[ENCAlgorithm] = JsonDecoder.string.mapOrFail(e => safeValueOf(ENCAlgorithm.valueOf(e)))
  given encoder: JsonEncoder[ENCAlgorithm] = JsonEncoder.string.contramap((e: ENCAlgorithm) => e.toString)
}

/** Key Wrapping Algorithms */
enum KWAlgorithm {
  case `ECDH-ES+A256KW` extends KWAlgorithm
  case `ECDH-1PU+A256KW` extends KWAlgorithm
}
object KWAlgorithm {
  given decoder: JsonDecoder[KWAlgorithm] = JsonDecoder.string.mapOrFail(e => safeValueOf(KWAlgorithm.valueOf(e)))
  given encoder: JsonEncoder[KWAlgorithm] = JsonEncoder.string.contramap((e: KWAlgorithm) => e.toString)
}
