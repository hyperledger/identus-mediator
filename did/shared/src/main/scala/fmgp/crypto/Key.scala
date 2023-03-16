package fmgp.crypto

import zio.json._
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import fmgp.util.{Base64, safeValueOf}

enum JWAAlgorithm:
  case ES256K extends JWAAlgorithm
  case ES256 extends JWAAlgorithm
  case ES384 extends JWAAlgorithm // TODO check https://identity.foundation/didcomm-messaging/spec/#algorithms
  case ES512 extends JWAAlgorithm // TODO check https://identity.foundation/didcomm-messaging/spec/#algorithms
  case EdDSA extends JWAAlgorithm
object JWAAlgorithm {
  given decoder: JsonDecoder[JWAAlgorithm] = JsonDecoder.string.mapOrFail(e => safeValueOf(JWAAlgorithm.valueOf(e)))
  given encoder: JsonEncoder[JWAAlgorithm] = JsonEncoder.string.contramap((e: JWAAlgorithm) => e.toString)
}

enum KTY:
  // case RSA extends KTY
  case EC extends KTY // Elliptic Curve
  case OKP extends KTY // Edwards-curve Octet Key Pair

object KTY {
  given decoder: JsonDecoder[KTY] = JsonDecoder.string.mapOrFail(e => safeValueOf(KTY.valueOf(e)))
  given encoder: JsonEncoder[KTY] = JsonEncoder.string.contramap((e: KTY) => e.toString)

  given decoderEC: JsonDecoder[KTY.EC.type] = JsonDecoder.string.mapOrFail(str =>
    if (str == KTY.EC.toString) Right(KTY.EC) else Left(s"'$str' is not a type KTY.EC")
  )
  given encoderEC: JsonEncoder[KTY.EC.type] = JsonEncoder.string.contramap((e: KTY.EC.type) => e.toString)

  given decoderOKP: JsonDecoder[KTY.OKP.type] = JsonDecoder.string.mapOrFail(str =>
    if (str == KTY.OKP.toString) Right(KTY.OKP) else Left(s"'$str' is not a type KTY.OKP")
  )
  given encoderOKP: JsonEncoder[KTY.OKP.type] = JsonEncoder.string.contramap((e: KTY.OKP.type) => e.toString)

}

enum Curve: // TODO make it type safe!
  case `P-256` extends Curve
  case `P-384` extends Curve
  case `P-521` extends Curve
  case secp256k1 extends Curve
  case X25519 extends Curve //  used for key exchange
  case Ed25519 extends Curve //  used for digital signatures

// sealed trait ECCurve // Elliptic Curve
type ECCurve = Curve.`P-256`.type | Curve.`P-384`.type | Curve.`P-521`.type | Curve.secp256k1.type
// sealed trait OKPCurve // Edwards-curve Octet Key Pair
type OKPCurve = Curve.X25519.type | Curve.Ed25519.type

object Curve {
  extension (curve: Curve) {

    /** asECCurve is a Unsafe methods!
      *
      * @throws ClassCastException
      *   is the curve is not a EC Curve
      */
    def asECCurve: ECCurve = curve match {
      case c: ECCurve  => c
      case c: OKPCurve => throw ClassCastException(s"Type $c is not a EC Curve")
    }

    /** asOKPCurve is a Unsafe methods!
      *
      * @throws ClassCastException
      *   is the curve is not a OKP Curve
      */
    def asOKPCurve: OKPCurve = curve match {
      case c: ECCurve  => throw ClassCastException(s"Type $c is not a OKP Curve")
      case c: OKPCurve => c
    }
  }

  extension (curve: ECCurve) {
    inline def isPointOnCurve(x: BigInt, y: BigInt) = curve match
      case Curve.`P-256`   => PointOnCurve.isPointOnCurveP_256(x, y)
      case Curve.`P-384`   => PointOnCurve.isPointOnCurveP_384(x, y)
      case Curve.`P-521`   => PointOnCurve.isPointOnCurveP_521(x, y)
      case Curve.secp256k1 => PointOnCurve.isPointOnCurveSecp256k1(x, y)
  }

  given decoder: JsonDecoder[Curve] = JsonDecoder.string.mapOrFail(e => safeValueOf(Curve.valueOf(e)))
  given encoder: JsonEncoder[Curve] = JsonEncoder.string.contramap((e: Curve) => e.toString)

  val ecCurveSet = Set(Curve.`P-256`, Curve.`P-384`, Curve.`P-521`, Curve.secp256k1)
  val okpCurveSet = Set(Curve.X25519, Curve.Ed25519)
}

/** https://tools.ietf.org/id/draft-ietf-jose-json-web-key-00.html */
sealed trait JWKObj {
  def kid: Option[String]
  def kty: KTY
  def alg: JWAAlgorithm
}

@jsonDiscriminator("kty")
// sealed trait OKP_EC_Key {
sealed abstract class OKP_EC_Key extends JWKObj {
  def kty: KTY // EC.type = KTY.EC
  def crv: Curve
  def kid: Option[String]
  def x: String
  def xNumbre = Base64.fromBase64url(x).decodeToBigInt

  // TODO // Should I make this type safe? Will add another dimension of types, just to move the error to the parser.
  assert(
    crv match {
      case Curve.secp256k1 => kty == KTY.EC
      case Curve.`P-256`   => kty == KTY.EC
      case Curve.`P-384`   => kty == KTY.EC
      case Curve.`P-521`   => kty == KTY.EC
      case Curve.X25519    => kty == KTY.OKP
      case Curve.Ed25519   => kty == KTY.OKP
    },
    s"$crv is not a $kty alg"
  )

  /** https://identity.foundation/didcomm-messaging/spec/#algorithms */
  def jwaAlgorithmtoSign: JWAAlgorithm = crv match {
    case Curve.secp256k1 => JWAAlgorithm.ES256K
    case Curve.`P-256`   => JWAAlgorithm.ES256 // (deprecated?)
    case Curve.`P-384`   => JWAAlgorithm.ES256 // (deprecated?) // TODO CHECK ES256
    case Curve.`P-521`   => JWAAlgorithm.ES256 // (deprecated?) // TODO CHECK ES256
    case Curve.X25519    => JWAAlgorithm.EdDSA
    case Curve.Ed25519   => JWAAlgorithm.EdDSA
  }

  def alg = crv match {
    case Curve.secp256k1 => JWAAlgorithm.ES256K
    case Curve.`P-256`   => JWAAlgorithm.ES256
    case Curve.`P-384`   => JWAAlgorithm.ES384
    case Curve.`P-521`   => JWAAlgorithm.ES512
    case Curve.X25519    => JWAAlgorithm.EdDSA
    case Curve.Ed25519   => JWAAlgorithm.EdDSA
  }

}

sealed abstract class OKPKey extends OKP_EC_Key {
  def getCurve: OKPCurve = crv.asOKPCurve
}
sealed abstract class ECKey extends OKP_EC_Key {
  def y: String
  def yNumbre = Base64.fromBase64url(y).decodeToBigInt
  def getCurve: ECCurve = crv.asECCurve
  def isPointOnCurve = getCurve.isPointOnCurve(xNumbre, yNumbre)
}

sealed trait PublicKey extends OKP_EC_Key
sealed trait PrivateKey extends OKP_EC_Key {
  def toPublicKey: PublicKey
  def d: String
}

case class OKPPublicKey(kty: KTY.OKP.type, crv: Curve, x: String, kid: Option[String]) extends OKPKey with PublicKey
case class OKPPrivateKey(kty: KTY.OKP.type, crv: Curve, d: String, x: String, kid: Option[String])
    extends OKPKey
    with PrivateKey {
  def toPublicKey: OKPPublicKey = OKPPublicKey(kty = kty, crv = crv, x = x, kid = kid)
}

case class ECPublicKey(kty: KTY.EC.type, crv: Curve, x: String, y: String, kid: Option[String])
    extends ECKey
    with PublicKey
case class ECPrivateKey(kty: KTY.EC.type, crv: Curve, d: String, x: String, y: String, kid: Option[String])
    extends ECKey
    with PrivateKey {
  def toPublicKey: ECPublicKey = ECPublicKey(kty = kty, crv = crv, x = x, y = y, kid = kid)
}

object PublicKey {

  given decoder: JsonDecoder[PublicKey] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst
      .get(JsonCursor.field("kty"))
      .flatMap(ast => KTY.decoder.fromJsonAST(ast))
      .flatMap {
        case KTY.EC =>
          // ECPublicKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ? see didJVM/testOnly *.KeySuite (parse Key with no kid)
          ECPublicKey.decoder.decodeJson(originalAst.toJson)
        case KTY.OKP =>
          // OKPPublicKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ? see didJVM/testOnly *.KeySuite (parse Key with no kid)
          OKPPublicKey.decoder.decodeJson(originalAst.toJson)
      }
  }

  given encoder: JsonEncoder[PublicKey] = new JsonEncoder[PublicKey] {
    override def unsafeEncode(b: PublicKey, indent: Option[Int], out: zio.json.internal.Write): Unit = b match {
      case obj: OKPPublicKey => OKPPublicKey.encoder.unsafeEncode(obj, indent, out)
      case obj: ECPublicKey  => ECPublicKey.encoder.unsafeEncode(obj, indent, out)
    }
  }

}

object PrivateKey {
  given decoder: JsonDecoder[PrivateKey] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst
      .get(JsonCursor.field("kty"))
      .flatMap { ast => KTY.decoder.fromJsonAST(ast) }
      .flatMap {
        case KTY.EC =>
          // ECPrivateKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ?
          ECPrivateKey.decoder.decodeJson(originalAst.toJson)
        case KTY.OKP =>
          // OKPPrivateKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ?
          OKPPrivateKey.decoder.decodeJson(originalAst.toJson)
      }
  }
  given encoder: JsonEncoder[PrivateKey] = new JsonEncoder[PrivateKey] {
    override def unsafeEncode(b: PrivateKey, indent: Option[Int], out: zio.json.internal.Write): Unit = b match {
      case obj: OKPPrivateKey => OKPPrivateKey.encoder.unsafeEncode(obj, indent, out)
      case obj: ECPrivateKey  => ECPrivateKey.encoder.unsafeEncode(obj, indent, out)
    }
  }
}

object ECPublicKey {
  given decoder: JsonDecoder[ECPublicKey] = DeriveJsonDecoder.gen[ECPublicKey]
  given encoder: JsonEncoder[ECPublicKey] = DeriveJsonEncoder.gen[ECPublicKey]
}
object ECPrivateKey {
  given decoder: JsonDecoder[ECPrivateKey] = DeriveJsonDecoder.gen[ECPrivateKey]
  given encoder: JsonEncoder[ECPrivateKey] = DeriveJsonEncoder.gen[ECPrivateKey]
}

object OKPPublicKey {
  given decoder: JsonDecoder[OKPPublicKey] = DeriveJsonDecoder.gen[OKPPublicKey]
  given encoder: JsonEncoder[OKPPublicKey] = DeriveJsonEncoder.gen[OKPPublicKey]
}
object OKPPrivateKey {
  given decoder: JsonDecoder[OKPPrivateKey] = DeriveJsonDecoder.gen[OKPPrivateKey]
  given encoder: JsonEncoder[OKPPrivateKey] = DeriveJsonEncoder.gen[OKPPrivateKey]
}
