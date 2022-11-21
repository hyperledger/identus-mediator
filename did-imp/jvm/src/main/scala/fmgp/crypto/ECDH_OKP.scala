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
import com.nimbusds.jose.crypto.impl.ECDH1PU
import com.nimbusds.jose.crypto.impl.CriticalHeaderParamsDeferral
import com.nimbusds.jose.JOSEException //TODO REMOVE
import javax.crypto.SecretKey

class ECDH_AnonOKP(
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

  val myProvider = ECDH_AnonCryptoProvider(curve)

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

    val updatedHeader: JWEHeader = new JWEHeader.Builder(header)
      .ephemeralPublicKey(ephemeralPublicKey)
      .build()

    val sharedSecrets = okpRecipientsKeys.map { case (vmr, key) =>
      (vmr, ECDH.deriveSharedSecret(key.toJWK, ephemeralPrivateKey))
    }

    myProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  }

  def decrypt(
      // header: JWEHeader,
      recipients: Seq[JWERecipient],
      iv: Base64URL,
      cipherText: Base64URL,
      authTag: Base64URL
  ): Array[Byte] = {

    val critPolicy: CriticalHeaderParamsDeferral = new CriticalHeaderParamsDeferral()
    critPolicy.ensureHeaderPasses(header)

    val ephemeralKey = Option(header.getEphemeralPublicKey)
      .map(_.asInstanceOf[OctetKeyPair])
      .getOrElse(throw new JOSEException("Missing ephemeral public key epk JWE header parameter"))

    val sharedSecrets = okpRecipientsKeys.map { case recipient: (VerificationMethodReferenced, OKPKey) =>
      val recipientKey = recipient._2.toJWK
      // TODO check point on curve
      val key = recipient._2.toJWK

      if (!key.getCurve().equals(ephemeralKey.getCurve())) {
        throw new JOSEException("Curve of ephemeral public key does not match curve of private key");
      }

      val use_the_defualt_JCA_Provider = null
      val Z = ECDH.deriveSharedSecret(
        ephemeralKey, // Public Key
        recipientKey, // Private Key
      )
      (recipient._1, Z)
    }

    myProvider.decryptAUX(
      header,
      sharedSecrets,
      recipients,
      iv,
      cipherText,
      authTag,
    )
  }
}

class ECDH_AuthOKP( // FIXME rename
    sender: OKPKey,
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

  val myProvider = ECDH_AuthCryptoProvider(curve)

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

    val updatedHeader: JWEHeader = new JWEHeader.Builder(header)
      .ephemeralPublicKey(ephemeralPublicKey)
      .build()

    val sharedSecrets = okpRecipientsKeys.map { case (vmr, key) =>
      val use_the_defualt_JCA_Provider = null
      (
        vmr,
        ECDH1PU.deriveSenderZ(
          sender.toJWK,
          key.toJWK,
          ephemeralPrivateKey,
        )
      )
    }

    myProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  }

  def decrypt(
      //  header: JWEHeader,
      recipients: Seq[JWERecipient],
      iv: Base64URL,
      cipherText: Base64URL,
      authTag: Base64URL
  ) = {

    val critPolicy: CriticalHeaderParamsDeferral = new CriticalHeaderParamsDeferral()

    critPolicy.ensureHeaderPasses(header);

    // Get ephemeral key from header
    val ephemeralPublicKey: OctetKeyPair = Option(header.getEphemeralPublicKey)
      .map(_.asInstanceOf[OctetKeyPair])
      .getOrElse(throw new JOSEException("Missing ephemeral public key epk JWE header parameter"))

    val sharedSecrets = okpRecipientsKeys.map { case recipient: (VerificationMethodReferenced, OKPKey) =>
      val recipientKey = recipient._2.toJWK
      // TODO check point on curve

      // if (!key.getCurve().equals(ephemeralKey.getCurve())) {
      //   throw new JOSEException("Curve of ephemeral public key does not match curve of private key");
      // }

      val use_the_defualt_JCA_Provider = null
      val Z: SecretKey = ECDH1PU.deriveRecipientZ(
        recipientKey,
        sender.toJWK.toPublicJWK(),
        ephemeralPublicKey
      )
      (recipient._1, Z)
    }

    myProvider.decryptAUX(header, sharedSecrets, recipients, iv, cipherText, authTag);
  }
}
