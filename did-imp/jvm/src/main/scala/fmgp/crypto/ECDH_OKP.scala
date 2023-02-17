package fmgp.crypto

import scala.util.Try
import scala.util.chaining._
import scala.collection.convert._
import scala.jdk.CollectionConverters._
import java.util.Collections

import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.crypto.impl.ECDH1PU
import com.nimbusds.jose.crypto.impl.CriticalHeaderParamsDeferral
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import javax.crypto.SecretKey

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm._
import fmgp.crypto.UtilsJVM.toJWKCurve
import fmgp.crypto.UtilsJVM.toJWK
import fmgp.util.Base64

import zio.json._

import fmgp.crypto.error._

trait ECDH_UtilsOKP {
  protected def getCurve(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)]
  ): Either[CurveError, Curve] = {
    okpRecipientsKeys.collect(_._2.getCurve).toSet match {
      case theCurve if theCurve.size == 1 =>
        if (Curve.okpCurveSet.contains(theCurve.head)) Right(theCurve.head)
        else Left(WrongCurve(theCurve.head, Curve.okpCurveSet))
      case multiCurves if multiCurves.size > 1 =>
        Left(MultiCurvesTypes(multiCurves, Curve.okpCurveSet))
      case zero if zero.size == 0 =>
        Left(MissingCurve(Curve.okpCurveSet))
    }
  }
}
//.toJWKCurve
object ECDH_AnonOKP extends ECDH_UtilsOKP {

  def encrypt(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: AnonHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] = for {
    curve <- getCurve(okpRecipientsKeys).map(_.toJWKCurve)
    myProvider = ECDH_AnonCryptoProvider(curve)

    // Generate ephemeral X25519 key pair
    ephemeralPrivateKeyBytes: Array[Byte] =
      com.google.crypto.tink.subtle.X25519.generatePrivateKey()
    ephemeralPublicKeyBytes: Array[Byte] =
      Try(com.google.crypto.tink.subtle.X25519.publicFromPrivate(ephemeralPrivateKeyBytes)).recover {
        case ex: java.security.InvalidKeyException =>
          // Should never happen since we just generated this private key
          throw ex // TODO
      }.get

    ephemeralPrivateKey: OctetKeyPair = // new OctetKeyPairGenerator(getCurve()).generate();
      new OctetKeyPair.Builder(curve, Base64.encode(ephemeralPublicKeyBytes))
        .d(Base64.encode(ephemeralPrivateKeyBytes))
        .build()
    ephemeralPublicKey: OctetKeyPair = ephemeralPrivateKey.toPublicJWK()
    ecKeyEphemeral <- ephemeralPublicKey.toJSONString().fromJson[OKPPublicKey].left.map(CryptoFailToParse(_))

    updatedHeader = header.buildWithKey(epk = ecKeyEphemeral)

    sharedSecrets = okpRecipientsKeys.map { case (vmr, key) =>
      (vmr, ECDH.deriveSharedSecret(key.toJWK, ephemeralPrivateKey))
    }

    ret = myProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  } yield (ret)

  def decrypt(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG
  ): Either[CryptoFailed, Array[Byte]] = for {
    curve <- getCurve(okpRecipientsKeys).map(_.toJWKCurve)
    myProvider = ECDH_AnonCryptoProvider(curve)
    critPolicy: CriticalHeaderParamsDeferral = {
      val aux = new CriticalHeaderParamsDeferral()
      aux.ensureHeaderPasses(header)
      aux
    }

    ephemeralPublicKey <- Option(header.getEphemeralPublicKey)
      .map(_.asInstanceOf[OctetKeyPair])
      .toRight(KeyMissingEpkJWEHeader)

    sharedSecrets <- CryptoErrorCollection.unfold {
      okpRecipientsKeys.map { case recipient: (VerificationMethodReferenced, OKPKey) =>
        val recipientKey = recipient._2.toJWK
        // TODO check point on curve
        val key = recipient._2.toJWK

        if (!key.getCurve().equals(ephemeralPublicKey.getCurve()))
          Left(PointNotOnCurve("Curve of ephemeral public key does not match curve of private key"))
        else
          Try(
            ECDH
              .deriveSharedSecret(
                ephemeralPublicKey, // Public Key
                recipientKey, // Private Key
              )
          ).toEither match {
            case Left(ex) => Left(SomeThrowable(ex))
            case Right(z) => Right((recipient._1, z))
          }
      }
    }

    ret <- myProvider.decryptAUX(
      header,
      sharedSecrets,
      recipients,
      iv,
      cipherText,
      authTag,
    )
  } yield (ret)
}

object ECDH_AuthOKP extends ECDH_UtilsOKP {

  def encrypt(
      sender: OKPKey,
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)], // TODO no empty seq
      header: AuthHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] = for {
    curve <- getCurve(okpRecipientsKeys).map(_.toJWKCurve)
    myProvider = ECDH_AuthCryptoProvider(curve)

    // Generate ephemeral X25519 key pair
    ephemeralPrivateKeyBytes: Array[Byte] =
      com.google.crypto.tink.subtle.X25519.generatePrivateKey()
    ephemeralPublicKeyBytes: Array[Byte] =
      Try(com.google.crypto.tink.subtle.X25519.publicFromPrivate(ephemeralPrivateKeyBytes)).recover {
        case ex: java.security.InvalidKeyException =>
          // Should never happen since we just generated this private key
          throw ex // TODO
      }.get

    ephemeralPrivateKey: OctetKeyPair = // new OctetKeyPairGenerator(getCurve()).generate();
      new OctetKeyPair.Builder(curve, Base64.encode(ephemeralPublicKeyBytes))
        .d(Base64.encode(ephemeralPrivateKeyBytes))
        .build();
    ephemeralPublicKey: OctetKeyPair = ephemeralPrivateKey.toPublicJWK()
    okpKeyEphemeral <- ephemeralPublicKey.toJSONString().fromJson[OKPPublicKey].left.map(CryptoFailToParse(_))

    updatedHeader = header.buildWithKey(okpKeyEphemeral)

    sharedSecrets = okpRecipientsKeys.map { case (vmr, key) =>
      (
        vmr,
        ECDH1PU.deriveSenderZ(
          sender.toJWK,
          key.toJWK,
          ephemeralPrivateKey,
        )
      )
    }

    ret = myProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  } yield (ret)

  def decrypt(
      sender: OKPKey,
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)], // TODO no empty seq
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG
  ): Either[CryptoFailed, Array[Byte]] = for {
    curve <- getCurve(okpRecipientsKeys).map(_.toJWKCurve)
    myProvider = ECDH_AuthCryptoProvider(curve)
    critPolicy: CriticalHeaderParamsDeferral = {
      val aux = new CriticalHeaderParamsDeferral()
      aux.ensureHeaderPasses(header);
      aux
    }

    // Get ephemeral key from header
    ephemeralPublicKey: OctetKeyPair <- Option(header.getEphemeralPublicKey)
      .map(_.asInstanceOf[OctetKeyPair])
      .toRight(KeyMissingEpkJWEHeader)

    sharedSecrets <- CryptoErrorCollection.unfold {
      okpRecipientsKeys.map { case recipient: (VerificationMethodReferenced, OKPKey) =>
        val recipientKey = recipient._2.toJWK
        // TODO check point on curve

        Try(
          ECDH1PU.deriveRecipientZ(
            recipientKey,
            sender.toJWK.toPublicJWK(),
            ephemeralPublicKey
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
