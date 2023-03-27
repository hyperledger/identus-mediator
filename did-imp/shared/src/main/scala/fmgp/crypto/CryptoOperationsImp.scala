package fmgp.crypto

import zio._
import zio.json._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.util.IOR
import fmgp.util.Base64

import scala.util.chaining._

/** https://identity.foundation/didcomm-messaging/spec/#key-wrapping-algorithms */
object CryptoOperationsImp extends CryptoOperations {

  override def sign(key: PrivateKey, plaintext: PlaintextMessage): IO[CryptoFailed, SignedMessage] =
    PlatformSpecificOperations.sign(key, plaintext)

  override def verify(key: PublicKey, jwm: SignedMessage): IO[CryptoFailed, Boolean] =
    PlatformSpecificOperations.verify(key, jwm)

  // ###############
  // ### encrypt ###
  // ###############

  override def anonEncrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] =
    recipientKidsKeys
      .groupBy(_._2.crv)
      .map {
        case (_: ECCurve, keys) =>
          anoncryptEC(keys.asInstanceOf[Seq[(VerificationMethodReferenced, ECKey)]], data)
        case (_: OKPCurve, keys) =>
          anoncryptOKP(keys.asInstanceOf[Seq[(VerificationMethodReferenced, OKPKey)]], data)
      }
      .headOption // TODO return multi messages!
      .getOrElse(ZIO.fail(NoKeys))

  // TODO accept the list of keys and return multi messages
  override def authEncrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] =
    (senderKidKey._2) match {
      case (ecSenderKey: ECKey) =>
        inline def recipientKeys = recipientKidsKeys
          .groupBy(_._2.crv)
          .get(ecSenderKey.crv)
          .toSeq
          .flatten
          .map(e => VerificationMethodReferencedWithKey(e._1.value, e._2.asInstanceOf[ECPublicKey]))
        authcryptEC((senderKidKey._1, ecSenderKey), recipientKeys, data)
      case okpSenderKey: OKPKey =>
        inline def recipientKeys = recipientKidsKeys
          .groupBy(_._2.crv)
          .get(okpSenderKey.crv)
          .toSeq
          .flatten
          .map(e => VerificationMethodReferencedWithKey(e._1.value, e._2.asInstanceOf[OKPPublicKey]))
        authcryptOKP((senderKidKey._1, okpSenderKey), recipientKeys, data)
    }

  // ### Methods ###

  def anoncryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      clearText: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] = {
    val header = AnonHeaderBuilder(
      apv = APV(ecRecipientsKeys.map(_._1)),
      enc = ENCAlgorithm.`A256CBC-HS512`,
      alg = KWAlgorithm.`ECDH-ES+A256KW`,
    )
    // ZIO.succeed(ECDH_AnonEC(ecRecipientsKeys, header).encrypt(clearText))
    ZIO.fromEither(ECDH.anonEncryptEC(ecRecipientsKeys, header, clearText))
  }

  def anoncryptOKP(
      okpRecipientKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      clearText: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] = {
    val header = AnonHeaderBuilder(
      apv = APV(okpRecipientKeys.map(_._1)),
      enc = ENCAlgorithm.`A256CBC-HS512`,
      alg = KWAlgorithm.`ECDH-ES+A256KW`,
    )
    // ZIO.succeed(ECDH_AnonOKP(okpRecipientKeys, header).encrypt(clearText))
    ZIO.fromEither(ECDH.anonEncryptOKP(okpRecipientKeys, header, clearText))
  }

  def authcryptEC(
      senderKidKey: (VerificationMethodReferenced, ECKey),
      recipientKeys: Seq[VerificationMethodReferencedWithKey[ECPublicKey]],
      clearText: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] = {
    val header = AuthHeaderBuilder(
      apv = APV(recipientKeys.map(_.vmr)),
      skid = senderKidKey._1,
      apu = APU(senderKidKey._1),
      enc = ENCAlgorithm.`A256CBC-HS512`,
      alg = KWAlgorithm.`ECDH-1PU+A256KW`,
    )
    ZIO.fromEither(ECDH.authEncryptEC(senderKidKey._2, recipientKeys, header, clearText))
  }

  def authcryptOKP(
      senderKidKey: (VerificationMethodReferenced, OKPKey),
      recipientKeys: Seq[VerificationMethodReferencedWithKey[OKPPublicKey]],
      clearText: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] = {
    val header = AuthHeaderBuilder(
      apv = APV(recipientKeys.map(_.vmr)),
      skid = senderKidKey._1,
      apu = APU(senderKidKey._1),
      enc = ENCAlgorithm.`A256CBC-HS512`,
      alg = KWAlgorithm.`ECDH-1PU+A256KW`,
    )
    ZIO.fromEither(ECDH.authEncryptOKP(senderKidKey._2, recipientKeys, header, clearText))
  }

  // ###############
  // ### decrypt ###
  // ###############

  def anonDecrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Array[Byte]] = {

    def header = msg.`protected`
    // String(Base64.fromBase64url(msg.`protected`).decode).fromJson[ProtectedHeader].toOption.get // FIXME

    val kids = msg.recipients.map(_.header.kid.value)
    recipientKidsKeys.filterNot(e => kids.contains(e._1)) match
      case Seq() => ZIO.fail(MissingDecryptionKey(kids: _*))
      case firstKey +: tail => {
        firstKey._2 match
          case ecKey: ECKey =>
            val jweRecipient =
              msg.recipients.map(recipient => JWERecipient(recipient.header.kid, recipient.encrypted_key))
            val ecRecipientsKeys = recipientKidsKeys.map(e => (e._1, e._2.asInstanceOf[ECPrivateKey])) // FIXME
            ZIO.fromEither(
              ECDH.anonDecryptEC(ecRecipientsKeys, header, jweRecipient, msg.iv, msg.ciphertext, msg.tag)
            )
          case okpKey: OKPKey =>
            val fixme = recipientKidsKeys.map(e => (e._1, e._2.asInstanceOf[OKPPrivateKey])) // FIXME
            val jweRecipient =
              msg.recipients.map(recipient => JWERecipient(recipient.header.kid, recipient.encrypted_key))
            ZIO.fromEither(
              ECDH.anonDecryptOKP(fixme, header, jweRecipient, msg.iv, msg.ciphertext, msg.tag)
            )
      }
  }

  def authDecrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Array[Byte]] = {
    def header = msg.`protected`
    // String(Base64.fromBase64url(msg.`protected`).decode).fromJson[ProtectedHeader].toOption.get // FIXME

    val jweRecipient =
      msg.recipients.map(recipient => JWERecipient(recipient.header.kid, recipient.encrypted_key))

    (senderKey) match {
      case ecSenderKey: ECKey =>
        ZIO
          .foreach(msg.recipients) { recipien =>
            recipientKidsKeys
              .find(_._1.value == recipien.header.kid.value)
              .pipe {
                case None                   => ZIO.succeed(Left(MissingDecryptionKey(recipien.header.kid.value)))
                case Some((id, key: ECKey)) => ZIO.succeed(Right((recipien.encrypted_key, id, key)))
                case Some(_)                => ZIO.fail(WrongKeysTypeCombination)
              }
          }
          .map(e =>
            e.foldLeft(
              IOR.Both[
                Option[MissingDecryptionKey],
                Seq[(Base64, VerificationMethodReferenced, ECKey)]
              ](None, Seq.empty)
            ) {
              case (IOR.Both(None, b), Left(newEx))        => IOR.Both(Some(newEx), b)
              case (IOR.Both(Some(oldEx), b), Left(newEx)) => IOR.Both(Some(oldEx + newEx), b)
              case (IOR.Both(a, b), Right(r))              => IOR.Both(a, b :+ r)
            }
          )
          .map { case IOR.Both(warnings, encryptedKey_vmr_keys) =>
            val ecRecipientsKeys = encryptedKey_vmr_keys.map { case (encryptedKey, vmr, keys) => (vmr, keys) }
            ECDH.authDecryptEC(ecSenderKey, ecRecipientsKeys, header, jweRecipient, msg.iv, msg.ciphertext, msg.tag)
          }
          .flatMap(ZIO.fromEither)
      case okpSenderKey: OKPKey =>
        ZIO
          .foreach(msg.recipients) { recipien =>
            recipientKidsKeys
              .find(_._1.value == recipien.header.kid.value)
              .pipe {
                case None                    => ZIO.succeed(Left(MissingDecryptionKey(kid = recipien.header.kid.value)))
                case Some((id, key: OKPKey)) => ZIO.succeed(Right((recipien.encrypted_key, id, key)))
                case Some(_)                 => ZIO.fail(WrongKeysTypeCombination)
              }
          }
          .map(e =>
            e.foldLeft(
              IOR.Both[
                Option[MissingDecryptionKey],
                Seq[(Base64, VerificationMethodReferenced, OKPKey)]
              ](None, Seq.empty)
            ) {
              case (IOR.Both(None, b), Left(newEx))        => IOR.Both(Some(newEx), b)
              case (IOR.Both(Some(oldEx), b), Left(newEx)) => IOR.Both(Some(oldEx + newEx), b)
              case (IOR.Both(a, b), Right(r))              => IOR.Both(a, b :+ r)
            }
          )
          .map { case IOR.Both(warnings, encryptedKey_vmr_keys) =>
            val okpRecipientsKeys = encryptedKey_vmr_keys.map { case (encryptedKey, vmr, keys) => (vmr, keys) }

            ECDH.authDecryptOKP(okpSenderKey, okpRecipientsKeys, header, jweRecipient, msg.iv, msg.ciphertext, msg.tag)
          }
          .flatMap(ZIO.fromEither)
    }
  }

}
