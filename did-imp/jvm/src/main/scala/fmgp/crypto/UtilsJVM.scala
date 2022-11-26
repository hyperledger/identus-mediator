package fmgp.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.util.StandardCharset

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm.EncryptedMessageGeneric
import fmgp.did.comm._
import fmgp.util._
import zio.json._

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._
import scala.collection.JavaConverters._

import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import fmgp.crypto.UtilsJVM.toJWK

given Conversion[ProtectedHeader, JWEHeader] with
  def apply(x: ProtectedHeader) = {
    val encryptionMethod = x.enc match
      case ENCAlgorithm.XC20P           => EncryptionMethod.XC20P
      case ENCAlgorithm.A256GCM         => EncryptionMethod.A256GCM
      case ENCAlgorithm.`A256CBC-HS512` => EncryptionMethod.A256CBC_HS512

    val algorithm = x.alg match
      case KWAlgorithm.`ECDH-ES+A256KW`  => JWEAlgorithm.ECDH_ES_A256KW
      case KWAlgorithm.`ECDH-1PU+A256KW` => JWEAlgorithm.ECDH_1PU_A256KW

    val tmp = new JWEHeader.Builder(algorithm, encryptionMethod)
      .`type`(JOSEObjectType(x.typ.typ))
      .agreementPartyVInfo(x.apv) // Utils.calculateAPV(x.apv  ecRecipientsKeys.map(_._1)))

    x.epk.foreach(e => tmp.ephemeralPublicKey(e.toJWK)) // new JWKECKey.Builder(curve, ephemeralPublicKey).build())
    x.skid.foreach(e => tmp.senderKeyID(e.value))
    x.apu.foreach(e => tmp.agreementPartyUInfo(e))
    tmp.build()
  }

given Conversion[Base64, com.nimbusds.jose.util.Base64URL] with
  def apply(x: Base64) = new com.nimbusds.jose.util.Base64URL(x.urlBase64)

object UtilsJVM {

  type Base64URLString = String // FIXME

  extension (alg: JWAAlgorithm) {
    def toJWSAlgorithm = alg match {
      case JWAAlgorithm.ES256K => JWSAlgorithm.ES256K
      case JWAAlgorithm.ES256  => JWSAlgorithm.ES256
      case JWAAlgorithm.ES384  => JWSAlgorithm.ES384
      case JWAAlgorithm.ES512  => JWSAlgorithm.ES512
      case JWAAlgorithm.EdDSA  => JWSAlgorithm.EdDSA
    }
  }
  extension (curve: Curve) {
    def toJWKCurve = curve match {
      case Curve.`P-256`   => JWKCurve.P_256
      case Curve.`P-384`   => JWKCurve.P_384
      case Curve.`P-521`   => JWKCurve.P_521
      case Curve.secp256k1 => JWKCurve.SECP256K1
      case Curve.X25519    => JWKCurve.X25519
      case Curve.Ed25519   => JWKCurve.Ed25519
    }
  }

  extension (ecKey: JWKECKey) {
    def verify(jwm: SignedMessage, alg: JWAAlgorithm): Boolean = {
      val _key = ecKey.toPublicJWK
      val verifier = new ECDSAVerifier(_key.toPublicJWK);
      val haeder = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(_key.getKeyID()).build()
      verifier.verify(
        haeder,
        (jwm.signatures.head.`protected` + "." + jwm.payload).getBytes(StandardCharset.UTF_8),
        Base64.fromBase64url(jwm.signatures.head.signature) // FIXME .head
      )
    }

    def sign(plaintext: PlaintextMessage, alg: JWAAlgorithm): SignedMessage = { // TODO use PlaintextMessage
      require(ecKey.isPrivate(), "EC JWK must include the private key (d)")

      val signer: JWSSigner = new ECDSASigner(ecKey) // Create the EC signer
      val haeder: JWSHeader = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(ecKey.getKeyID()).build()
      val payloadObj = new Payload(plaintext.toJson)
      val jwsObject: JWSObject = new JWSObject(haeder, payloadObj) // Creates the JWS object with payload

      jwsObject.sign(signer)
      jwsObject.serialize().split('.') match {
        case Array(protectedValue, payload, signature) =>
          assert(payload == payloadObj.toBase64URL.toString) // redundant check
          assert(signature == jwsObject.getSignature.toString) // redundant check
          SignedMessage(
            payload = payload,
            Seq(JWMSignatureObj(`protected` = `protectedValue`, signature = signature)) // TODO haeder
          )
      }
    }
  }

  extension (okpKey: OctetKeyPair) {
    def verify(jwm: SignedMessage, alg: JWAAlgorithm): Boolean = {
      val _key = okpKey.toPublicJWK
      val verifier = new Ed25519Verifier(_key.toPublicJWK);
      val haeder = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(_key.getKeyID()).build()
      verifier.verify(
        haeder,
        (jwm.signatures.head.`protected` + "." + jwm.payload).getBytes(StandardCharset.UTF_8),
        Base64.fromBase64url(jwm.signatures.head.signature) // FIXME .head
      )
    }

    def sign(plaintext: PlaintextMessage, alg: JWAAlgorithm): SignedMessage = { // TODO use PlaintextMessage
      require(okpKey.isPrivate(), "EC JWK must include the private key (d)")

      val signer: JWSSigner = new Ed25519Signer(okpKey) // Create the OKP signer
      val haeder: JWSHeader = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(okpKey.getKeyID()).build()
      val payloadObj = new Payload(plaintext.toJson)

      val jwsObject: JWSObject = new JWSObject(haeder, payloadObj) // Creates the JWS object with payload

      jwsObject.sign(signer)
      jwsObject.serialize().split('.') match {
        case Array(protectedValue, payload, signature) =>
          assert(payload == payloadObj.toBase64URL.toString) // redundant check
          assert(signature == jwsObject.getSignature.toString) // redundant check
          SignedMessage(
            payload = payload,
            Seq(JWMSignatureObj(`protected` = `protectedValue`, signature = signature)) // TODO haeder
          )
      }
    }
  }

  extension (key: OKP_EC_Key) {
    def toJWK: JWKECKey | OctetKeyPair = {
      key match {
        case ec: ECKey   => ec.toJWK
        case okp: OKPKey => okp.toJWK
      }
    }
  }

  extension (ec: ECKey) {
    def toJWK: JWKECKey = {

      val builder = ec.getCurve match {
        case c: Curve.`P-256`.type =>
          JWKECKey.Builder(c.toJWKCurve, Base64.fromBase64url(ec.x), Base64.fromBase64url(ec.y))
        case c: Curve.`P-384`.type =>
          JWKECKey.Builder(c.toJWKCurve, Base64.fromBase64url(ec.x), Base64.fromBase64url(ec.y))
        case c: Curve.`P-521`.type =>
          JWKECKey.Builder(c.toJWKCurve, Base64.fromBase64url(ec.x), Base64.fromBase64url(ec.y))
        case c: Curve.secp256k1.type =>
          JWKECKey.Builder(c.toJWKCurve, Base64.fromBase64url(ec.x), Base64.fromBase64url(ec.y))
      }
      ec.kid.foreach(builder.keyID)
      ec match { // for private key
        case _: PublicKey  => // ok (just the public key)
        case k: PrivateKey => builder.d(Base64.fromBase64url(k.d))
      }
      builder.build()
    }
  }
  extension (okp: OKPKey) {
    def toJWK: OctetKeyPair = {
      val builder = okp.getCurve match {
        case c: Curve.Ed25519.type => OctetKeyPair.Builder(c.toJWKCurve, Base64.fromBase64url(okp.x))
        case c: Curve.X25519.type  => OctetKeyPair.Builder(c.toJWKCurve, Base64.fromBase64url(okp.x))
      }
      okp.kid.foreach(builder.keyID)
      okp match { // for private key
        case _: PublicKey  => // ok (just the public key)
        case k: PrivateKey => builder.d(Base64.fromBase64url(k.d))
      }
      builder.build()
    }
  }

  extension (key: PrivateKey) {
    def verify(jwm: SignedMessage): Future[Boolean] = Future.successful(
      key.toJWK match {
        case ecKey: JWKECKey      => ecKey.verify(jwm, key.jwaAlgorithmtoSign)
        case okpKey: OctetKeyPair => okpKey.verify(jwm, key.jwaAlgorithmtoSign)
      }
    )

  }

  extension (key: OKP_EC_Key) {
    def sign(plaintext: PlaintextMessageClass): Future[SignedMessage] =
      Future.successful( // TODO use PlaintextMessageClass
        key.toJWK match {
          case ecKey: JWKECKey      => ecKey.sign(plaintext, key.jwaAlgorithmtoSign)
          case okpKey: OctetKeyPair => okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        }
      )
  }

}
