package fmgp.crypto

import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.Payload
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.crypto.X25519Encrypter
import com.nimbusds.jose.crypto.ECDH1PUEncrypter
import com.nimbusds.jose.crypto.ECDH1PUX25519Encrypter
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.util.StandardCharset
import com.nimbusds.jose.util.Base64URL

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm._
import fmgp.crypto.UtilsJVM.toJWKCurve
import fmgp.crypto.UtilsJVM.toJWK

import zio.json._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._
import scala.collection.convert._
import scala.collection.JavaConverters._

import java.util.Collections

class MyX25519Encrypter(
    okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
    header: JWEHeader,
    // alg: JWEAlgorithm = JWEAlgorithm.ECDH_ES_A256KW,
    // enc: EncryptionMethod = EncryptionMethod.A256CBC_HS512
) {

  val curve = okpRecipientsKeys.collect(_._2.getCurve).toSet match {
    case theCurve if theCurve.size == 1 =>
      assert(Curve.okpCurveSet.contains(theCurve.head), "Curve not expected") // FIXME ERROR
      theCurve.head.toJWKCurve
    case _ => ??? // FIXME ERROR
  }

  val myECDHCryptoProvider = MyECDHCryptoProvider(curve)

  /** TODO return errors:
    *   - com.nimbusds.jose.JOSEException: Invalid ephemeral public EC key: Point(s) not on the expected curve
    *   - com.nimbusds.jose.JOSEException: Couldn't unwrap AES key: Integrity check failed
    */
  def encrypt(clearText: Array[Byte]): EncryptedMessageGeneric = {
    // Generate ephemeral X25519 key pair
    val ephemeralPrivateKeyBytes: Array[Byte] =
      com.google.crypto.tink.subtle.X25519.generatePrivateKey()
    var ephemeralPublicKeyBytes: Array[Byte] =
      Try(com.google.crypto.tink.subtle.X25519.publicFromPrivate(ephemeralPrivateKeyBytes)).recover {
        case ex: java.security.InvalidKeyException =>
          // Should never happen since we just generated this private key
          throw ex // new JOSEException(eex.getMessage(), ex);
      }.get

    val ephemeralPrivateKey: OctetKeyPair = // new OctetKeyPairGenerator(getCurve()).generate();
      new OctetKeyPair.Builder(curve, Base64URL.encode(ephemeralPublicKeyBytes))
        .d(Base64URL.encode(ephemeralPrivateKeyBytes))
        .build();
    val ephemeralPublicKey: OctetKeyPair = ephemeralPrivateKey.toPublicJWK()

    // val header: JWEHeader = new JWEHeader.Builder(alg, enc)
    //   .ephemeralPublicKey(ephemeralPublicKey) // Add the ephemeral public EC key to the header
    //   .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
    //   .agreementPartyVInfo(Utils.calculateAPV(okpRecipientsKeys.map(_._1)))
    //   .build()
    val updatedHeader: JWEHeader = new JWEHeader.Builder(header)
      .ephemeralPublicKey(ephemeralPublicKey)
      .build()

    val sharedSecrets = okpRecipientsKeys.map { case (vmr, key) =>
      (vmr, ECDH.deriveSharedSecret(key.toJWK, ephemeralPrivateKey))
    }

    myECDHCryptoProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  }
}
