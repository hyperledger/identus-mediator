package fmgp.crypto

import com.nimbusds.jose.UnprotectedHeader
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWERecipient
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Pair
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.crypto.ECDH1PUEncrypterMulti
import com.nimbusds.jose.crypto.ECDH1PUX25519EncrypterMulti
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.crypto.ECDH1PUDecrypter
import com.nimbusds.jose.crypto.ECDH1PUX25519Decrypter
import com.nimbusds.jose.crypto.ECDH1PUDecrypterMulti
import com.nimbusds.jose.crypto.ECDH1PUX25519DecrypterMulti
import com.nimbusds.jose.crypto.X25519DecrypterMulti
import com.nimbusds.jose.crypto.ECDHDecrypterMulti

import zio._
import zio.json._

import fmgp.crypto.UtilsJVM._
import fmgp.crypto.error._
import fmgp.data.IOR
import fmgp.did._
import fmgp.did.comm._

import scala.util.chaining._
import scala.collection.JavaConverters._
import org.bouncycastle.crypto.engines.AESWrapEngine

object RawOperations extends CryptoOperations {

  override def sign(key: PrivateKey, plaintext: PlaintextMessageClass): UIO[SignedMessage] =
    ZIO.succeed( // TODO use PlaintextMessageClass
      key.toJWK match {
        case ecKey: JWKECKey      => ecKey.sign(plaintext, key.jwaAlgorithmtoSign)
        case okpKey: OctetKeyPair => okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
      }
    )

  override def verify(key: OKP_EC_Key, jwm: SignedMessage): UIO[Boolean] = ZIO.succeed(
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
  ): UIO[EncryptedMessageGeneric] =
    recipientKidsKeys
      .foldRight(
        (Seq.empty[(VerificationMethodReferenced, ECKey)], Seq.empty[(VerificationMethodReferenced, OKPKey)])
      ) { (e, acu) =>
        e._2 match
          case ecKey: ECKey   => (acu._1 :+ (e._1, ecKey), acu._2)
          case okpKey: OKPKey => (acu._1, acu._2 :+ (e._1, okpKey))
      }
      .pipe(e => (e._1.sortBy(_._1.value), e._2.sortBy(_._1.value))) // order recipients by name
      .pipe {
        case (Seq(), Seq())    => ??? // FIXME ERROR
        case (ecKeys, Seq())   => anoncryptEC(ecKeys, data)
        case (Seq(), okpKeys)  => anoncryptOKP(okpKeys, data)
        case (ecKeys, okpKeys) => ??? // FIXME ERROR
      }

  override def authEncrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): UIO[EncryptedMessageGeneric] =
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
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {

    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512) // XC20P)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .agreementPartyVInfo(Utils.calculateAPV(ecRecipientsKeys.map(_._1)))
      .build()
    ZIO.succeed(MyECEncrypter(ecRecipientsKeys, header).encrypt(clearText))
  }

  def anoncryptOKP( // anoncryptECDH_X25519
      okpRecipientKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {
    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .agreementPartyVInfo(Utils.calculateAPV(okpRecipientKeys.map(_._1)))
      .build()
    ZIO.succeed(MyX25519Encrypter(okpRecipientKeys, header).encrypt(clearText))
  }

  def authcryptEC( // encrypterECDH1PU
      senderKidKey: (VerificationMethodReferenced, JWKECKey),
      recipientKeys: Seq[(VerificationMethodReferenced, JWKECKey)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {
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

    ZIO.succeed(
      EncryptedMessageGeneric(
        ciphertext = parts.getCipherText().toString, // : Base64URL,
        `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
        recipients = recipients, // auxRecipient.toSeq,
        tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
        iv = parts.getInitializationVector().toString // : InitializationVector
      )
    )

  }

  def authcryptOKP( // encrypterECDH1PU_X25519
      senderKidKey: (VerificationMethodReferenced, OctetKeyPair),
      recipientKeys: Seq[(VerificationMethodReferenced, OctetKeyPair)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {
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

    ZIO.succeed(
      EncryptedMessageGeneric(
        ciphertext = parts.getCipherText().toString, // : Base64URL,
        `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
        recipients = recipients, // auxRecipient.toSeq,
        tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
        iv = parts.getInitializationVector().toString // : InitializationVector
      )
    )
  }

  // ###############
  // ### decrypt ###
  // ###############

  def anonDecrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`))
    val kids = msg.recipients.map(_.header.kid.value)
    val aaa = recipientKidsKeys.filterNot(e => kids.contains(e._1))
    val hhh = aaa.head // FIXME use headOption
    hhh._2 match {
      case ecKey: ECKey =>
        val pairs = recipientKidsKeys.map { case (vmr, key: ECKey) => // FIXME check all keys
          Pair.of(
            UnprotectedHeader.Builder().keyID(vmr.value).build(),
            key.toJWK
          )
        }

        val jweRecipient = msg.recipients.map(recipient =>
          JWERecipient(
            UnprotectedHeader.Builder().keyID(recipient.header.kid.value).build(),
            Base64URL(recipient.encrypted_key)
          )
        )

        val ret = ECDHDecrypterMulti(pairs.toList.asJava)
          .decrypt(
            header,
            jweRecipient.toList.asJava,
            Base64URL(msg.iv),
            Base64URL(msg.ciphertext),
            Base64URL(msg.tag)
          )
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))

      case okpKey: OKPKey =>
        val pairs = recipientKidsKeys.map { case (vmr, key: OKPKey) => // FIXME check all keys
          Pair.of(
            UnprotectedHeader.Builder().keyID(vmr.value).build(),
            key.toJWK
          )
        }

        val jweRecipient = msg.recipients.map(recipient =>
          JWERecipient(
            UnprotectedHeader.Builder().keyID(recipient.header.kid.value).build(),
            Base64URL(recipient.encrypted_key)
          )
        )

        val ret = X25519DecrypterMulti(pairs.toList.asJava)
          .decrypt(
            header,
            jweRecipient.toList.asJava,
            Base64URL(msg.iv),
            Base64URL(msg.ciphertext),
            Base64URL(msg.tag)
          )
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))
    }

  }

  override def anonDecryptOne(
      key: PrivateKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = {
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
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))
      case okpKey: OctetKeyPair => // okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        val ret = X25519Decrypter(okpKey).decrypt(
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))
    }
  }

  override def authDecryptOne(
      recipientKey: PrivateKey,
      senderKey: PublicKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`))
    (recipientKey.toJWK, senderKey.toJWK) match {
      case (ecKey: JWKECKey, ecKeyToVerify: JWKECKey) =>
        val ret = ECDH1PUDecrypter(ecKey.toECPrivateKey(), ecKeyToVerify.toECPublicKey()).decrypt( // FIXME
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))

      case (okpKey: OctetKeyPair, okpKeyToVerify: OctetKeyPair) => // okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        val ret = ECDH1PUX25519Decrypter(okpKey, okpKeyToVerify).decrypt(
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))
      case _ => ZIO.fail(WrongKeysTypeCombination)
    }
  }

  override def authDecrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`)) // TODO REMOVE

    val jweRecipient = msg.recipients.map(recipient =>
      JWERecipient(
        UnprotectedHeader.Builder().keyID(recipient.header.kid.value).build(),
        Base64URL(recipient.encrypted_key)
      )
    )

    (senderKey).toJWK match {
      case ecSenderKey: JWKECKey =>
        ZIO
          .foreach(msg.recipients) { recipien =>
            recipientKidsKeys
              .find(_._1.value == recipien.header.kid.value)
              .pipe {
                case None                   => ZIO.succeed(Left(MissDecryptionKey(kid = recipien.header.kid.value)))
                case Some((id, key: ECKey)) => ZIO.succeed(Right((recipien.encrypted_key, id, key)))
                case Some(_)                => ZIO.fail(WrongKeysTypeCombination)
              }
          }
          .map(e =>
            e.foldLeft(
              IOR.Both[Seq[MissDecryptionKey], Seq[(String, VerificationMethodReferenced, ECKey)]](
                Seq.empty,
                Seq.empty
              )
            ) {
              case (IOR.Both(a, b), Left(l))  => IOR.Both(a :+ l, b)
              case (IOR.Both(a, b), Right(r)) => IOR.Both(a, b :+ r)
            }
          )
          .map { case IOR.Both(warnings, encryptedKey_vmr_keys) =>
            val pairs = encryptedKey_vmr_keys.map { case (encryptedKey, vmr, keys) =>
              Pair.of(
                UnprotectedHeader.Builder().keyID(vmr.value).build(),
                keys.toJWK
              )
            }

            ECDH1PUDecrypterMulti(ecSenderKey, pairs.toList.asJava)
              .decrypt(
                header,
                jweRecipient.toList.asJava,
                Base64URL(msg.iv),
                Base64URL(msg.ciphertext),
                Base64URL(msg.tag)
              )
          }
          .flatMap(ret => ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_))))
      case okpSenderKey: OctetKeyPair =>
        ZIO
          .foreach(msg.recipients) { recipien =>
            recipientKidsKeys
              .find(_._1.value == recipien.header.kid.value)
              .pipe {
                case None                    => ZIO.succeed(Left(MissDecryptionKey(kid = recipien.header.kid.value)))
                case Some((id, key: OKPKey)) => ZIO.succeed(Right((recipien.encrypted_key, id, key)))
                case Some(_)                 => ZIO.fail(WrongKeysTypeCombination)
              }
          }
          .map(e =>
            e.foldLeft(
              IOR.Both[Seq[MissDecryptionKey], Seq[(String, VerificationMethodReferenced, OKPKey)]](
                Seq.empty,
                Seq.empty
              )
            ) {
              case (IOR.Both(a, b), Left(l))  => IOR.Both(a :+ l, b)
              case (IOR.Both(a, b), Right(r)) => IOR.Both(a, b :+ r)
            }
          )
          .map { case IOR.Both(warnings, encryptedKey_vmr_keys) =>
            val pairs = encryptedKey_vmr_keys.map { case (encryptedKey, vmr, keys) =>
              Pair.of(
                UnprotectedHeader.Builder().keyID(vmr.value).build(),
                keys.toJWK
              )
            }

            ECDH1PUX25519DecrypterMulti(okpSenderKey, pairs.toList.asJava)
              .decrypt(
                header,
                jweRecipient.toList.asJava,
                Base64URL(msg.iv),
                Base64URL(msg.ciphertext),
                Base64URL(msg.tag)
              )
          }
          .flatMap(ret => ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_))))
    }
  }

}
