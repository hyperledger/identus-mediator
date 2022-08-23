package fmgp.crypto

import com.nimbusds.jose.UnprotectedHeader
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.crypto.ECDHEncrypterMulti
import com.nimbusds.jose.util.Pair
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.crypto.ECDH1PUEncrypterMulti
import com.nimbusds.jose.crypto.ECDH1PUX25519EncrypterMulti
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.crypto.ECDH1PUDecrypter
import com.nimbusds.jose.crypto.ECDH1PUX25519Decrypter

import fmgp.crypto.UtilsJVM._
import fmgp.did._
import fmgp.did.comm._

import scala.util.chaining._
import scala.collection.JavaConverters._
import scala.concurrent.Future

object RawOperations extends CryptoOperations {

  override def sign(key: PrivateKey, plaintext: PlaintextMessageClass): Future[SignedMessage] =
    Future.successful( // TODO use PlaintextMessageClass
      key.toJWK match {
        case ecKey: JWKECKey      => ecKey.sign(plaintext, key.jwaAlgorithmtoSign)
        case okpKey: OctetKeyPair => okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
      }
    )

  override def verify(key: OKP_EC_Key, jwm: SignedMessage): Future[Boolean] = Future.successful(
    key.toJWK match {
      case ecKey: JWKECKey      => ecKey.verify(jwm, key.jwaAlgorithmtoSign)
      case okpKey: OctetKeyPair => okpKey.verify(jwm, key.jwaAlgorithmtoSign)
    }
  )

  // ###############
  // ### encrypt ###
  // ###############
  // TODO https://identity.foundation/didcomm-messaging/spec/#key-wrapping-algorithms
  // REMOVE https://bitbucket.org/connect2id/nimbus-jose-jwt/branch/release-9.16?dest=release-9.16-preview.1

  override def anonEncrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): EncryptedMessageGeneric =
    recipientKidsKeys
      .foldRight(
        (Seq.empty[(VerificationMethodReferenced, JWKECKey)], Seq.empty[(VerificationMethodReferenced, OctetKeyPair)])
      ) { (e, acu) =>
        e._2.toJWK match
          case ecKey: JWKECKey      => (acu._1 :+ (e._1, ecKey), acu._2)
          case okpKey: OctetKeyPair => (acu._1, acu._2 :+ (e._1, okpKey))
      }
      .pipe(e => (e._1.sortBy(_._1.value), e._2.sortBy(_._1.value))) // order recipients by name
      .pipe {
        case (Seq(), Seq())    => ??? // FIXME
        case (ecKeys, Seq())   => anoncryptEC(ecKeys, data)
        case (Seq(), okpKeys)  => anoncryptOKP(okpKeys, data)
        case (ecKeys, okpKeys) => ??? // FIXME
      }

  override def authEncrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): EncryptedMessageGeneric =
    (senderKidKey._2).toJWK match {
      case (ecSenderKey: JWKECKey) =>
        val recipientKeys = recipientKidsKeys.map {
          case (vmr, key: ECPublicKey)  => (vmr, key.toJWK)
          case (vmr, key: OKPPublicKey) => ??? // FIXME
        }
        authcryptEC((senderKidKey._1, ecSenderKey), recipientKeys, data)
      case okpSenderKey: OctetKeyPair =>
        val recipientKeys = recipientKidsKeys.map {
          case (vmr, key: ECPublicKey)  => ??? // FIXME
          case (vmr, key: OKPPublicKey) => (vmr, key.toJWK)
        }
        authcryptOKP((senderKidKey._1, okpSenderKey), recipientKeys, data)
    }

  // ### Methods ###

  def anoncryptEC( // anoncryptECDH
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, JWKECKey)],
      clearText: Array[Byte]
  ) = {
    val aux = ecRecipientsKeys.map(e =>
      Pair.of(
        UnprotectedHeader.Builder().keyID(e._1.value).build(),
        e._2
      )
    )
    val encrypter = ECDHEncrypterMulti(aux.asJava)

    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512) // XC20P)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .agreementPartyVInfo(Utils.calculateAPV(ecRecipientsKeys.map(_._1)))
      .build()
    val parts = encrypter.encrypt(header, clearText)
    val recipients = parts.getRecipients.asScala.toSeq.map { e =>
      Recipient(e.getEncryptedKey().toString(), RecipientHeader(VerificationMethodReferenced(e.getHeader().getKeyID())))
    }

    EncryptedMessageGeneric(
      ciphertext = parts.getCipherText().toString, // : Base64URL,
      `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
      recipients = recipients, // auxRecipient.toSeq,
      tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
      iv = parts.getInitializationVector().toString // : InitializationVector
    )
  }

  def anoncryptOKP( // anoncryptECDH_X25519
      okpRecipientKeys: Seq[(VerificationMethodReferenced, OctetKeyPair)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {
    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .agreementPartyVInfo(Utils.calculateAPV(okpRecipientKeys.map(_._1)))
      .build()
    MyX25519Encrypter(okpRecipientKeys, header).encrypt(clearText)
  }

  def authcryptEC( // encrypterECDH1PU
      senderKidKey: (VerificationMethodReferenced, JWKECKey),
      recipientKeys: Seq[(VerificationMethodReferenced, JWKECKey)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {
    val aux = recipientKeys.map(e =>
      Pair.of(
        UnprotectedHeader.Builder().keyID(e._1.value).build(),
        e._2
      )
    )
    val encrypter = ECDH1PUEncrypterMulti(senderKidKey._2, aux.asJava)

    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_1PU_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .senderKeyID(senderKidKey._1.value)
      .agreementPartyUInfo(Utils.calculateAPU(senderKidKey._1))
      .agreementPartyVInfo(Utils.calculateAPV(recipientKeys.map(_._1)))
      .build()

    val parts = encrypter.encrypt(header, clearText)
    val recipients = parts.getRecipients.asScala.toSeq
      .map { e =>
        Recipient(
          e.getEncryptedKey().toString(),
          RecipientHeader(VerificationMethodReferenced(e.getHeader().getKeyID()))
        )
      }

    EncryptedMessageGeneric(
      ciphertext = parts.getCipherText().toString, // : Base64URL,
      `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
      recipients = recipients, // auxRecipient.toSeq,
      tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
      iv = parts.getInitializationVector().toString // : InitializationVector
    )

  }

  def authcryptOKP( // encrypterECDH1PU_X25519
      senderKidKey: (VerificationMethodReferenced, OctetKeyPair),
      recipientKeys: Seq[(VerificationMethodReferenced, OctetKeyPair)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {
    val aux = recipientKeys.map(e =>
      Pair.of(
        UnprotectedHeader.Builder().keyID(e._1.value).build(),
        e._2
      )
    )

    val encrypter = ECDH1PUX25519EncrypterMulti(senderKidKey._2, aux.asJava)

    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_1PU_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .senderKeyID(senderKidKey._1.value)
      .agreementPartyUInfo(Utils.calculateAPU(senderKidKey._1))
      .agreementPartyVInfo(Utils.calculateAPV(recipientKeys.map(_._1)))
      .build()
    val parts = encrypter.encrypt(header, clearText)
    val recipients = parts.getRecipients.asScala.toSeq
      .map { e =>
        Recipient(
          e.getEncryptedKey().toString(),
          RecipientHeader(VerificationMethodReferenced(e.getHeader().getKeyID()))
        )
      }

    EncryptedMessageGeneric(
      ciphertext = parts.getCipherText().toString, // : Base64URL,
      `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
      recipients = recipients, // auxRecipient.toSeq,
      tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
      iv = parts.getInitializationVector().toString // : InitializationVector
    )
  }

  // ###############
  // ### decrypt ###
  // ###############

  override def anonDecrypt(
      key: PrivateKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): Future[String] = Future.successful {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`))
    key.toJWK match {
      case ecKey: JWKECKey =>
        val ret = ECDHDecrypter(ecKey).decrypt(
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
      case okpKey: OctetKeyPair => // okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        val ret = X25519Decrypter(okpKey).decrypt(
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
    }
  }

  override def authDecrypt(
      keyDecrypt: PrivateKey,
      keyToVerify: PublicKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): Future[String] = Future.successful {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`))
    (keyDecrypt.toJWK, keyToVerify.toJWK) match {
      case (ecKey: JWKECKey, ecKeyToVerify: JWKECKey) =>
        val ret = ECDH1PUDecrypter(ecKey.toECPrivateKey(), ecKeyToVerify.toECPublicKey()).decrypt( // FIXME
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
      case (okpKey: OctetKeyPair, okpKeyToVerify: OctetKeyPair) => // okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        val ret = ECDH1PUX25519Decrypter(okpKey, okpKeyToVerify) decrypt (
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
      case _ => throw new AssertionError("The Keys must be of the same type!") // FIXME Make function type safe!
    }
  }

}
