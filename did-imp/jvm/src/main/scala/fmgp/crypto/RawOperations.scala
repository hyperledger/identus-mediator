package fmgp.crypto

import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL

import zio._
import zio.json._

import fmgp.crypto.UtilsJVM._
import fmgp.crypto.error._
import fmgp.data.IOR
import fmgp.did._
import fmgp.did.comm._

import scala.util.chaining._
import scala.collection.JavaConverters._

/** https://identity.foundation/didcomm-messaging/spec/#key-wrapping-algorithms */
object RawOperations extends CryptoOperations {

  override def sign(key: PrivateKey, plaintext: PlaintextMessage): UIO[SignedMessage] =
    ZIO.succeed(
      key.toJWK match {
        case ecKey: JWKECKey      => ecKey.sign(plaintext, key.jwaAlgorithmtoSign)
        case okpKey: OctetKeyPair => okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
      }
    )

  override def verify(key: OKP_EC_Key, jwm: SignedMessage): UIO[Boolean] =
    ZIO.succeed(
      key.toJWK match {
        case ecKey: JWKECKey      => ecKey.verify(jwm, key.jwaAlgorithmtoSign)
        case okpKey: OctetKeyPair => okpKey.verify(jwm, key.jwaAlgorithmtoSign)
      }
    )

  // ###############
  // ### encrypt ###
  // ###############

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
    (senderKidKey._2) match {
      case (ecSenderKey: ECKey) =>
        val recipientKeys = recipientKidsKeys.map {
          case (vmr, key: ECPublicKey)  => (vmr, key)
          case (vmr, key: OKPPublicKey) => ??? // FIXME
        }
        authcryptEC((senderKidKey._1, ecSenderKey), recipientKeys, data)
      case okpSenderKey: OKPKey =>
        val recipientKeys = recipientKidsKeys.map {
          case (vmr, key: ECPublicKey)  => ??? // FIXME
          case (vmr, key: OKPPublicKey) => (vmr, key)
        }
        authcryptOKP((senderKidKey._1, okpSenderKey), recipientKeys, data)
    }

  // ### Methods ###

  def anoncryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {
    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType(MediaTypes.ANONCRYPT.typ))
      .agreementPartyVInfo(Utils.calculateAPV(ecRecipientsKeys.map(_._1)))
      .build()
    ZIO.succeed(ECDH_AnonEC(ecRecipientsKeys, header).encrypt(clearText))
  }

  def anoncryptOKP(
      okpRecipientKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {
    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType(MediaTypes.ANONCRYPT.typ))
      .agreementPartyVInfo(Utils.calculateAPV(okpRecipientKeys.map(_._1)))
      .build()
    ZIO.succeed(ECDH_AnonOKP(okpRecipientKeys, header).encrypt(clearText))
  }

  def authcryptEC(
      senderKidKey: (VerificationMethodReferenced, ECKey),
      recipientKeys: Seq[(VerificationMethodReferenced, ECKey)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {
    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_1PU_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType(MediaTypes.AUTHCRYPT.typ))
      .senderKeyID(senderKidKey._1.value)
      .agreementPartyUInfo(Utils.calculateAPU(senderKidKey._1))
      .agreementPartyVInfo(Utils.calculateAPV(recipientKeys.map(_._1)))
      .build()
    ZIO.succeed(ECDH_AuthEC(senderKidKey._2, recipientKeys, header).encrypt(clearText))
  }

  def authcryptOKP(
      senderKidKey: (VerificationMethodReferenced, OKPKey),
      recipientKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      clearText: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = {
    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_1PU_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType(MediaTypes.AUTHCRYPT.typ))
      .senderKeyID(senderKidKey._1.value)
      .agreementPartyUInfo(Utils.calculateAPU(senderKidKey._1))
      .agreementPartyVInfo(Utils.calculateAPV(recipientKeys.map(_._1)))
      .build()
    ZIO.succeed(ECDH_AuthOKP(senderKidKey._2, recipientKeys, header).encrypt(clearText))
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
    val allKeysUsedOnMsg = recipientKidsKeys.filterNot(e => kids.contains(e._1))

    allKeysUsedOnMsg.head._2 match {
      case ecKey: ECKey =>
        val jweRecipient =
          msg.recipients.map(recipient => JWERecipient(recipient.header.kid, Base64URL(recipient.encrypted_key)))
        val fixme = recipientKidsKeys.map(e => (e._1, e._2.asInstanceOf[ECPrivateKey])) // FIXME
        val ret = ECDH_AnonEC(fixme, header)
          .decrypt(
            jweRecipient,
            Base64URL(msg.iv),
            Base64URL(msg.ciphertext),
            Base64URL(msg.tag)
          )
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))

      case okpKey: OKPKey =>
        val fixme = recipientKidsKeys.map(e => (e._1, e._2.asInstanceOf[OKPPrivateKey])) // FIXME
        val jweRecipient =
          msg.recipients.map(recipient => JWERecipient(recipient.header.kid, Base64URL(recipient.encrypted_key)))

        val ret = ECDH_AnonOKP(fixme, header)
          .decrypt(
            jweRecipient,
            Base64URL(msg.iv),
            Base64URL(msg.ciphertext),
            Base64URL(msg.tag)
          )
        ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_)))
    }

  }

  override def authDecrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message] = {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`))

    val jweRecipient =
      msg.recipients.map(recipient => JWERecipient(recipient.header.kid, Base64URL(recipient.encrypted_key)))

    (senderKey) match {
      case ecSenderKey: ECKey =>
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
            val ecRecipientsKeys = encryptedKey_vmr_keys.map { case (encryptedKey, vmr, keys) => (vmr, keys) }

            ECDH_AuthEC(ecSenderKey, ecRecipientsKeys, header)
              .decrypt(
                jweRecipient,
                Base64URL(msg.iv),
                Base64URL(msg.ciphertext),
                Base64URL(msg.tag)
              )
          }
          .flatMap(ret => ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_))))
      case okpSenderKey: OKPKey =>
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
            val okpRecipientsKeys = encryptedKey_vmr_keys.map { case (encryptedKey, vmr, keys) => (vmr, keys) }

            ECDH_AuthOKP(okpSenderKey, okpRecipientsKeys, header)
              .decrypt(
                jweRecipient,
                Base64URL(msg.iv),
                Base64URL(msg.ciphertext),
                Base64URL(msg.tag)
              )
          }
          .flatMap(ret => ZIO.fromEither(String(ret).fromJson[Message].left.map(FailToParse(_))))
    }
  }

}
