package fmgp

import fmgp.did.comm.PlaintextMessageClass
import typings.jose.joseBooleans
import typings.jose.joseRequire
import typings.jose.joseStrings
import typings.jose.mod.{ECCurve => ECCurveJS}
import typings.jose.mod.{OKPCurve => OKPCurveJS}
import typings.jose.mod.JWKECKey
import typings.jose.mod.JWKOKPKey
import typings.jose.mod.JWS
import typings.jose.mod.JWS.VerifyOptions
import typings.jose.mod.JWT
import typings.jose.mod.errors.JWSVerificationFailed
import zio.json._

import scala.scalajs.js
import scala.scalajs.js.JavaScriptException
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._

package object crypto {

  // extension (alg: JWAAlgorithm) {
  //   def toJS = alg match {
  //     case JWAAlgorithm.ES256K => ECCurveJS.secp256k1
  //     case JWAAlgorithm.ES256  => ECCurveJS.`P-256`
  //     case JWAAlgorithm.EdDSA  => OKPCurveJS.Ed25519 // EdDSA
  //   }
  // }

  extension (ec: ECKey) {
    def toJWKECKey = {
      val jwkECKey = ec.getCurve match {
        case Curve.`P-256`   => JWKECKey.apply(ECCurveJS.`P-256`, ec.x, ec.y)
        case Curve.`P-384`   => JWKECKey.apply(ECCurveJS.`P-384`, ec.x, ec.y)
        case Curve.`P-521`   => JWKECKey.apply(ECCurveJS.`P-521`, ec.x, ec.y)
        case Curve.secp256k1 => JWKECKey.apply(ECCurveJS.secp256k1, ec.x, ec.y)
      }
      ec.kid.foreach(id => jwkECKey.setKid(id))
      ec match {
        case _: PublicKey  => // ok
        case k: PrivateKey => jwkECKey.setD(k.d)
      }
      jwkECKey
    }
  }

  extension (okp: OKPKey) {
    def toJWKOKPKey = {
      val jwkOKPKey = okp.getCurve match {
        case Curve.Ed25519 => JWKOKPKey.apply(OKPCurveJS.Ed25519, okp.x)
        case Curve.X25519  => JWKOKPKey.apply(OKPCurveJS.X25519, okp.x)
      }
      okp.kid.foreach(id => jwkOKPKey.setKid(id))
      okp match {
        case _: PublicKey  => // ok
        case k: PrivateKey => jwkOKPKey.setD(k.d)
      }
      jwkOKPKey
    }
  }

  extension (key: OKP_EC_Key) {

    private def toJWK: JWKECKey | JWKOKPKey = key match {
      case ec: ECKey   => ec.toJWKECKey
      case okp: OKPKey => okp.toJWKOKPKey
    }

    def verify(jwm: JWM): Boolean = Try {
      key match {
        case ec: ECKey   => JWT.verify(jwm.base64, ec.toJWKECKey)
        case okp: OKPKey => JWT.verify(jwm.base64, okp.toJWKOKPKey)
      }
    } match {
      case Success(i)                                              => true
      case Failure(JavaScriptException(ex: JWSVerificationFailed)) => false
      case Failure(ex)                                             => throw ex
    }
  }

  extension (key: PrivateKey) {
    def sign(plaintext: PlaintextMessageClass): JWM = { // TODO use PlaintextMessage
      val data = js.JSON.parse(plaintext.toJson).asInstanceOf[js.Object]
      val serialize =
        key match {
          case ec: ECKey   => JWT.sign(data, ec.toJWKECKey)
          case okp: OKPKey => JWT.sign(data, okp.toJWKOKPKey)
        }

      serialize.split('.') match {
        case Array(protectedValue, payload, signature) =>
          JWM(
            payload = payload,
            Seq(JWMSignatureObj(`protected` = `protectedValue`, signature = signature)) // TODO haeder
          )
      }

    }
  }

}
