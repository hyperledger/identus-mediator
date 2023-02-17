package fmgp.crypto

import scala.util.Try
import scala.util.chaining._
import scala.collection.convert._
import scala.jdk.CollectionConverters._

import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.crypto.impl.ECDH1PU
import com.nimbusds.jose.crypto.impl.ECDH1PUCryptoProvider
import com.nimbusds.jose.crypto.impl.CriticalHeaderParamsDeferral
import com.nimbusds.jose.crypto.utils.ECChecks
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import javax.crypto.SecretKey

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm._
import fmgp.crypto.UtilsJVM.toJWKCurve
import fmgp.crypto.UtilsJVM.toJWK
import fmgp.util.Base64

import java.util.Collections
import zio.json._
import fmgp.crypto.error._

trait ECDH_UtilsEC {
  protected def getCurve(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)]
  ): Either[CurveError, Curve] =
    ecRecipientsKeys.collect(_._2.getCurve).toSet match {
      case theCurve if theCurve.size == 1 =>
        if (Curve.ecCurveSet.contains(theCurve.head)) Right(theCurve.head)
        else Left(WrongCurve(theCurve.head, Curve.okpCurveSet))
      case multiCurves if multiCurves.size > 1 =>
        Left(MultiCurvesTypes(multiCurves, Curve.okpCurveSet))
      case zero if zero.size == 0 =>
        Left(MissingCurve(Curve.okpCurveSet))
    }
}

object ECDH_AnonEC extends ECDH_UtilsEC {

  def encrypt(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: AnonHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] = for {
    curve <- getCurve(ecRecipientsKeys).map(_.toJWKCurve)
    myProvider = new ECDH_AnonCryptoProvider(curve)

    // Generate ephemeral EC key pair
    ephemeralKeyPair: JWKECKey = new ECKeyGenerator(curve).generate()
    ephemeralPublicKey = ephemeralKeyPair.toECPublicKey()
    ephemeralPrivateKey = ephemeralKeyPair.toECPrivateKey()
    ecKeyEphemeral <- ephemeralKeyPair.toJSONString().fromJson[ECPublicKey].left.map(CryptoFailToParse(_))

    updatedHeader = header.buildWithKey(epk = ecKeyEphemeral)

    sharedSecrets = ecRecipientsKeys.map { case (vmr, key) =>
      val use_the_defualt_JCA_Provider = null
      (vmr, ECDH.deriveSharedSecret(key.toJWK.toECPublicKey(), ephemeralPrivateKey, use_the_defualt_JCA_Provider))
    }

    ret = myProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  } yield (ret)

  def decrypt(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG
  ): Either[CryptoFailed, Array[Byte]] = for {
    curve <- getCurve(ecRecipientsKeys).map(_.toJWKCurve)
    myProvider = new ECDH_AnonCryptoProvider(curve)
    critPolicy: CriticalHeaderParamsDeferral = {
      val aux = new CriticalHeaderParamsDeferral()
      aux.ensureHeaderPasses(header)
      aux
    }

    // Get ephemeral EC key
    ephemeralKey <- Option(header.getEphemeralPublicKey)
      .map(_.asInstanceOf[JWKECKey])
      .toRight(KeyMissingEpkJWEHeader)

    sharedSecrets <- CryptoErrorCollection.unfold {
      ecRecipientsKeys.map { case recipient: (VerificationMethodReferenced, ECKey) =>
        val recipientKey = recipient._2.toJWK

        if (!ECChecks.isPointOnCurve(ephemeralKey.toECPublicKey(), recipientKey.toECPrivateKey()))
          Left(PointNotOnCurve("Curve of ephemeral public key does not match curve of private key"))
        else
          Try(
            ECDH.deriveSharedSecret(
              ephemeralKey.toECPublicKey(),
              recipientKey.toECPrivateKey(),
              null /*use_the_defualt_JCA_Provider*/
            )
          ).toEither match {
            case Left(ex) => Left(SomeThrowable(ex))
            case Right(z) => Right((recipient._1, z))
          }
      }
    }

    ret <- myProvider.decryptAUX(header, sharedSecrets, recipients, iv, cipherText, authTag)
  } yield (ret)
}

object ECDH_AuthEC extends ECDH_UtilsEC {

  def encrypt(
      sender: ECKey,
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: AuthHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] = for {
    curve <- getCurve(ecRecipientsKeys).map(_.toJWKCurve)
    myProvider = new ECDH_AuthCryptoProvider(curve)

    // Generate ephemeral EC key pair on the same curve as the consumer's public key
    ephemeralKeyPair: JWKECKey = new ECKeyGenerator(curve).generate()
    ephemeralPublicKey = ephemeralKeyPair.toECPublicKey()
    ephemeralPrivateKey = ephemeralKeyPair.toECPrivateKey()
    ecKeyEphemeral <- ephemeralKeyPair.toJSONString().fromJson[ECPublicKey].left.map(CryptoFailToParse(_))

    // Add the ephemeral public EC key to the header
    updatedHeader = header.buildWithKey(ecKeyEphemeral)

    sharedSecrets = ecRecipientsKeys.map { case (vmr, key) =>
      val use_the_defualt_JCA_Provider = null
      (
        vmr,
        ECDH1PU.deriveSenderZ(
          sender.toJWK.toECPrivateKey(),
          key.toJWK.toECPublicKey(),
          ephemeralPrivateKey,
          myProvider.getJCAContext().getKeyEncryptionProvider()
        )
      )
    }

    ret = myProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  } yield (ret)

  def decrypt(
      sender: ECKey,
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG
  ): Either[CryptoFailed, Array[Byte]] = for {
    curve <- getCurve(ecRecipientsKeys).map(_.toJWKCurve)
    myProvider = new ECDH_AuthCryptoProvider(curve)
    critPolicy: CriticalHeaderParamsDeferral = {
      val aux = new CriticalHeaderParamsDeferral()
      aux.ensureHeaderPasses(header)
      aux
    }
    // Get ephemeral EC key
    ephemeralKey <- Option(header.getEphemeralPublicKey)
      .map(_.asInstanceOf[JWKECKey])
      .toRight(KeyMissingEpkJWEHeader)

    sharedSecrets <- CryptoErrorCollection.unfold {
      ecRecipientsKeys.map { case recipient: (VerificationMethodReferenced, ECKey) =>
        val recipientKey = recipient._2.toJWK

        if (!ECChecks.isPointOnCurve(ephemeralKey.toECPublicKey(), recipientKey.toECPrivateKey()))
          Left(PointNotOnCurve("Curve of ephemeral public key does not match curve of private key"))
        else
          Try(
            ECDH1PU.deriveRecipientZ(
              recipientKey.toECPrivateKey,
              sender.toJWK.toECPublicKey,
              ephemeralKey.toECPublicKey,
              null /*use_the_defualt_JCA_Provider*/
            )
          ).toEither match {
            case Left(ex) => Left(SomeThrowable(ex))
            case Right(z) => Right((recipient._1, z))
          }
      }
    }

    ret <- myProvider.decryptAUX(header, sharedSecrets, recipients, iv, cipherText, authTag)
  } yield (ret)
}
