package fmgp.crypto

import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.crypto.impl.ECDHCryptoProvider
import com.nimbusds.jose.crypto.impl.ContentCryptoProvider
import com.nimbusds.jose.crypto.impl.AESKW
import com.nimbusds.jose.crypto.impl.ECDH1PU
import com.nimbusds.jose.crypto.impl.ECDH1PUCryptoProvider
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.StandardCharset
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.JWECryptoParts
import javax.crypto.SecretKey

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm.EncryptedMessageGeneric
import fmgp.did.comm.Recipient
import fmgp.did.comm.RecipientHeader

import java.util.Collections
import scala.collection.JavaConverters._

/** Elliptic-curve Diffie–Hellman */
case class ECDH_AnonCryptoProvider(val curve: JWKCurve) extends ECDHCryptoProvider(curve) {

  override def supportedEllipticCurves(): java.util.Set[JWKCurve] = Set(curve).asJava

  /** @throws JOSEException
    */
  def encryptAUX(
      header: JWEHeader,
      sharedSecrets: Seq[(fmgp.did.VerificationMethodReferenced, javax.crypto.SecretKey)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {

    val algMode: ECDH.AlgorithmMode = ECDH.resolveAlgorithmMode(header.getAlgorithm);
    assert(algMode == ECDH.AlgorithmMode.KW)

    val cek: SecretKey = ContentCryptoProvider.generateCEK(
      header.getEncryptionMethod,
      getJCAContext.getSecureRandom
    )

    sharedSecrets match {
      case head :: tail =>
        val headParts: JWECryptoParts = encryptWithZ(header, head._2, clearText, cek)

        val recipients = tail.map { rs =>
          val sharedKey: SecretKey = ECDH.deriveSharedKey(header, rs._2, getConcatKDF)
          val encryptedKey = Base64URL.encode(AESKW.wrapCEK(cek, sharedKey, getJCAContext.getKeyEncryptionProvider))
          (rs._1, encryptedKey)
        }

        val auxRecipient = ((head._1, headParts.getEncryptedKey) +: recipients)
          .map(e => Recipient(e._2.toString, RecipientHeader(e._1)))

        EncryptedMessageGeneric(
          ciphertext = headParts.getCipherText().toString, // : Base64URL,
          `protected` = Base64URL.encode(headParts.getHeader().toString).toString(), // : Base64URLHeaders,
          recipients = auxRecipient.toSeq,
          tag = headParts.getAuthenticationTag().toString, // AuthenticationTag,
          iv = headParts.getInitializationVector().toString // : InitializationVector
        )
    }

  }

  def decryptAUX(
      header: JWEHeader,
      sharedSecrets: Seq[(VerificationMethodReferenced, SecretKey)],
      recipients: Seq[JWERecipient],
      iv: Base64URL,
      cipherText: Base64URL,
      authTag: Base64URL,
  ): Array[Byte] = {

    val result = sharedSecrets.map { case (vmr, secretKey) =>
      recipients
        .find(recipient => recipient.vmr == vmr)
        .map(_.encryptedKey)
        .map(encryptedKey => decryptWithZ(header, secretKey, encryptedKey, iv, cipherText, authTag))
    }.flatten

    assert(result.tail.forall(_.sameElements(result.head)), "FIXME DECRYPT multi (diferent) stuff")

    result.head
  }

}

/** Elliptic-curve Diffie–Hellman */
case class ECDH_AuthCryptoProvider(val curve: JWKCurve) extends ECDH1PUCryptoProvider(curve) {

  override def supportedEllipticCurves(): java.util.Set[JWKCurve] = Set(curve).asJava

  def encryptAUX(
      header: JWEHeader,
      sharedSecrets: Seq[(fmgp.did.VerificationMethodReferenced, javax.crypto.SecretKey)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {

    val algMode: ECDH.AlgorithmMode = ECDH1PU.resolveAlgorithmMode(header.getAlgorithm())
    assert(algMode == ECDH.AlgorithmMode.KW)

    val cek: SecretKey = ContentCryptoProvider.generateCEK(
      header.getEncryptionMethod,
      getJCAContext.getSecureRandom
    )

    sharedSecrets match {
      case head :: tail =>
        val headParts: JWECryptoParts = encryptWithZ(header, head._2, clearText, cek)

        val recipients = tail.map { rs =>
          val sharedKey: SecretKey =
            ECDH1PU.deriveSharedKey(header, rs._2, headParts.getAuthenticationTag, getConcatKDF)
          val encryptedKey = Base64URL.encode(AESKW.wrapCEK(cek, sharedKey, getJCAContext.getKeyEncryptionProvider))
          (rs._1, encryptedKey)
        }

        val auxRecipient = ((head._1, headParts.getEncryptedKey) +: recipients)
          .map(e => Recipient(e._2.toString(), RecipientHeader(e._1)))

        EncryptedMessageGeneric(
          ciphertext = headParts.getCipherText().toString, // : Base64URL,
          `protected` = Base64URL.encode(headParts.getHeader().toString).toString(), // : Base64URLHeaders,
          recipients = auxRecipient.toSeq,
          tag = headParts.getAuthenticationTag().toString, // AuthenticationTag,
          iv = headParts.getInitializationVector().toString // : InitializationVector
        )
    }
  }

  def decryptAUX(
      header: JWEHeader,
      sharedSecrets: Seq[(VerificationMethodReferenced, SecretKey)],
      recipients: Seq[JWERecipient],
      iv: Base64URL,
      cipherText: Base64URL,
      authTag: Base64URL
  ) = {

    val result = sharedSecrets.map { case (vmr, secretKey) =>
      recipients
        .find(recipient => recipient.vmr == vmr)
        .map(_.encryptedKey)
        .map(encryptedKey => decryptWithZ(header, secretKey, encryptedKey, iv, cipherText, authTag))
    }.flatten
    // META DATA
    assert(result.tail.forall(_.sameElements(result.head)), "FIXME DECRYPT multi (diferent) stuff")

    result.head
  }

}
