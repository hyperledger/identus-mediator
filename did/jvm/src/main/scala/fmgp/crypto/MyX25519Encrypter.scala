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
import com.nimbusds.jose.crypto.ECDH1PUDecrypter
import com.nimbusds.jose.crypto.ECDH1PUX25519Decrypter
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.crypto.X25519Encrypter
import com.nimbusds.jose.crypto.ECDH1PUEncrypter
import com.nimbusds.jose.crypto.ECDH1PUX25519Encrypter
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.StandardCharset
import com.nimbusds.jose.util.Base64URL

import fmgp.did.comm.EncryptedMessageGeneric
import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm._

import zio.json._
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._
import scala.collection.convert._
import scala.collection.JavaConverters._

import com.nimbusds.jose.crypto.impl.ECDHCryptoProvider
import com.nimbusds.jose.UnprotectedHeader
import javax.crypto.SecretKey
import com.nimbusds.jose.JWECryptoParts
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.crypto.impl.ContentCryptoProvider
import com.nimbusds.jose.crypto.impl.AESKW

import java.util.Collections
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator

abstract class MyECDHCryptoProvider(curve: JWKCurve) extends ECDHCryptoProvider(curve) {

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

}

class MyX25519Encrypter(
    okpRecipientsKeys: Seq[(VerificationMethodReferenced, OctetKeyPair)],
    header: JWEHeader,
    // alg: JWEAlgorithm = JWEAlgorithm.ECDH_ES_A256KW,
    // enc: EncryptionMethod = EncryptionMethod.A256CBC_HS512
) extends MyECDHCryptoProvider(JWKCurve.X25519) {

  override def supportedEllipticCurves(): java.util.Set[JWKCurve] =
    java.util.Collections.singleton(JWKCurve.X25519);

  def encrypt(clearText: Array[Byte]): EncryptedMessageGeneric = {

    // Generate ephemeral X25519 key pair
    val ephemeralPrivateKeyBytes: Array[Byte] = com.google.crypto.tink.subtle.X25519.generatePrivateKey();
    var ephemeralPublicKeyBytes: Array[Byte] =
      Try(com.google.crypto.tink.subtle.X25519.publicFromPrivate(ephemeralPrivateKeyBytes)).recover {
        case ex: java.security.InvalidKeyException =>
          // Should never happen since we just generated this private key
          throw ex // new JOSEException(eex.getMessage(), ex);
      }.get

    val ephemeralPrivateKey: OctetKeyPair = // new OctetKeyPairGenerator(getCurve()).generate();
      new OctetKeyPair.Builder(JWKCurve.X25519, Base64URL.encode(ephemeralPublicKeyBytes))
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
      (vmr, ECDH.deriveSharedSecret(key, ephemeralPrivateKey))
    }

    encryptAUX(updatedHeader, sharedSecrets, clearText)

  }
}
