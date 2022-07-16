package fmgp.crypto

import zio.json._
import zio.json.ast.Json
import zio.json.ast.JsonCursor

enum JWAAlgorithm:
  case ES256K extends JWAAlgorithm
  case ES256 extends JWAAlgorithm
  case ES384 extends JWAAlgorithm // TODO check https://identity.foundation/didcomm-messaging/spec/#algorithms
  case ES512 extends JWAAlgorithm // TODO check https://identity.foundation/didcomm-messaging/spec/#algorithms
  case EdDSA extends JWAAlgorithm

enum KTY:
  // case RSA extends KTY
  case EC extends KTY // Elliptic Curve
  case OKP extends KTY // Edwards-curve Octet Key Pair

object KTY {
  implicit val decoder: JsonDecoder[KTY] = JsonDecoder.string.map(KTY.valueOf)
  implicit val encoder: JsonEncoder[KTY] = JsonEncoder.string.contramap((e: KTY) => e.toString)

  implicit val decoderEC: JsonDecoder[KTY.EC.type] = JsonDecoder.string.mapOrFail(str =>
    if (str == KTY.EC.toString) Right(KTY.EC) else Left(s"'$str' is not a type KTY.EC")
  )
  implicit val encoderEC: JsonEncoder[KTY.EC.type] = JsonEncoder.string.contramap((e: KTY.EC.type) => e.toString)

  implicit val decoderOKP: JsonDecoder[KTY.OKP.type] = JsonDecoder.string.mapOrFail(str =>
    if (str == KTY.OKP.toString) Right(KTY.OKP) else Left(s"'$str' is not a type KTY.OKP")
  )
  implicit val encoderOKP: JsonEncoder[KTY.OKP.type] = JsonEncoder.string.contramap((e: KTY.OKP.type) => e.toString)

}

enum Curve: // TODO make it type safe!
  case `P-256` extends Curve
  case `P-384` extends Curve
  case `P-521` extends Curve
  case secp256k1 extends Curve
  case X25519 extends Curve
  case Ed25519 extends Curve
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
  implicit val decoder: JsonDecoder[Curve] = JsonDecoder.string.map(Curve.valueOf)
  implicit val encoder: JsonEncoder[Curve] = JsonEncoder.string.contramap((e: Curve) => e.toString)
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
    case Curve.secp256k1 => JWAAlgorithm.ES256K //  ES256K
    case Curve.`P-256`   => JWAAlgorithm.ES256 // ES256
    case Curve.`P-384`   => JWAAlgorithm.ES384 // ES384
    case Curve.`P-521`   => JWAAlgorithm.ES512 // ES512
    case Curve.X25519    => JWAAlgorithm.EdDSA
    case Curve.Ed25519   => JWAAlgorithm.EdDSA
  }

}

sealed abstract class OKPKey extends OKP_EC_Key {
  def getCurve: OKPCurve = crv.asOKPCurve
}
sealed abstract class ECKey extends OKP_EC_Key {
  def y: String
  def getCurve: ECCurve = crv.asECCurve
}

sealed trait PublicKey extends OKP_EC_Key
sealed trait PrivateKey extends OKP_EC_Key {
  def toPublicKey: PublicKey
  def d: String
}

case class OKPPublicKey(kty: KTY.OKP.type, crv: Curve, x: String, kid: Option[String] = None)
    extends OKPKey
    with PublicKey
case class OKPPrivateKey(kty: KTY.OKP.type, crv: Curve, d: String, x: String, kid: Option[String] = None)
    extends OKPKey
    with PrivateKey {
  def toPublicKey = OKPPublicKey(kty = kty, crv = crv, x = x)
}

case class ECPublicKey(kty: KTY.EC.type, crv: Curve, x: String, y: String, kid: Option[String] = None)
    extends ECKey
    with PublicKey
case class ECPrivateKey(kty: KTY.EC.type, crv: Curve, d: String, x: String, y: String, kid: Option[String] = None)
    extends ECKey
    with PrivateKey {
  def toPublicKey = ECPublicKey(kty = kty, crv = crv, x = x, y = y)
}

object PublicKey {

  implicit val decoder: JsonDecoder[PublicKey] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst
      .get(JsonCursor.field("kty"))
      .flatMap(ast => KTY.decoder.fromJsonAST(ast))
      .flatMap {
        case KTY.EC  => ECPublicKey.decoder.fromJsonAST(originalAst).map(e => e: PublicKey)
        case KTY.OKP => OKPPublicKey.decoder.fromJsonAST(originalAst).map(e => e: PublicKey)
      }
  }
  implicit val encoder: JsonEncoder[PublicKey] = DeriveJsonEncoder.gen[PublicKey]
}

object PrivateKey {
  implicit val decoder: JsonDecoder[PrivateKey] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst
      .get(JsonCursor.field("kty"))
      .flatMap { ast => KTY.decoder.fromJsonAST(ast) }
      .flatMap {
        case KTY.EC  => ECPrivateKey.decoder.fromJsonAST(originalAst)
        case KTY.OKP => OKPPrivateKey.decoder.fromJsonAST(originalAst)
      }
  }
  implicit val encoder: JsonEncoder[PrivateKey] = DeriveJsonEncoder.gen[PrivateKey]
}

object ECPublicKey {
  implicit val decoder: JsonDecoder[ECPublicKey] = DeriveJsonDecoder.gen[ECPublicKey]
  implicit val encoder: JsonEncoder[ECPublicKey] = DeriveJsonEncoder.gen[ECPublicKey]
}
object ECPrivateKey {
  implicit val decoder: JsonDecoder[ECPrivateKey] = DeriveJsonDecoder.gen[ECPrivateKey]
  implicit val encoder: JsonEncoder[ECPrivateKey] = DeriveJsonEncoder.gen[ECPrivateKey]
}

object OKPPublicKey {
  implicit val decoder: JsonDecoder[OKPPublicKey] = DeriveJsonDecoder.gen[OKPPublicKey]
  implicit val encoder: JsonEncoder[OKPPublicKey] = DeriveJsonEncoder.gen[OKPPublicKey]
}
object OKPPrivateKey {
  implicit val decoder: JsonDecoder[OKPPrivateKey] = DeriveJsonDecoder.gen[OKPPrivateKey]
  implicit val encoder: JsonEncoder[OKPPrivateKey] = DeriveJsonEncoder.gen[OKPPrivateKey]
}
