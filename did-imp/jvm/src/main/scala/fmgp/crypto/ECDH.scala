package fmgp.crypto

import fmgp.did.VerificationMethodReferenced
import fmgp.did.VerificationMethodReferencedWithKey
import fmgp.did.comm._
import fmgp.crypto.error._

object ECDH {
  def anonEncryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: AnonHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    ECDH_AnonEC.encrypt(ecRecipientsKeys, header, clearText)

  def anonDecryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    ECDH_AnonEC.decrypt(ecRecipientsKeys, header, recipients, iv, cipherText, authTag)

  def authEncryptEC(
      sender: ECKey,
      ecRecipientsKeys: Seq[VerificationMethodReferencedWithKey[ECPublicKey]],
      header: AuthHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    ECDH_AuthEC.encrypt(sender, ecRecipientsKeys.map(_.pair), header, clearText)

  def authDecryptEC(
      sender: ECKey,
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    ECDH_AuthEC.decrypt(sender, ecRecipientsKeys, header, recipients, iv, cipherText, authTag)

  def anonEncryptOKP(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: AnonHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    ECDH_AnonOKP.encrypt(okpRecipientsKeys, header, clearText)

  def anonDecryptOKP(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    ECDH_AnonOKP.decrypt(okpRecipientsKeys, header, recipients, iv, cipherText, authTag)

  def authEncryptOKP(
      sender: OKPKey,
      okpRecipientsKeys: Seq[VerificationMethodReferencedWithKey[OKPPublicKey]],
      header: AuthHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    ECDH_AuthOKP.encrypt(sender, okpRecipientsKeys.map(_.pair), header, clearText)

  def authDecryptOKP(
      sender: OKPKey,
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    ECDH_AuthOKP.decrypt(sender, okpRecipientsKeys, header, recipients, iv, cipherText, authTag)
}
