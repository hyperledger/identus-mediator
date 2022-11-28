package fmgp.crypto

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm._
import fmgp.crypto.error._

object ECDH {
  def anonEncryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeader,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Right(ECDH_AnonEC(ecRecipientsKeys, header).encrypt(clearText))

  def anonDecryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeader,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Right(ECDH_AnonEC(ecRecipientsKeys, header).decrypt(recipients, iv, cipherText, authTag))

  def authEncryptEC(
      sender: ECKey,
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeader,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Right(ECDH_AuthEC(sender, ecRecipientsKeys, header).encrypt(clearText))

  def authDecryptEC(
      sender: ECKey,
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeader,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Right(ECDH_AuthEC(sender, ecRecipientsKeys, header).decrypt(recipients, iv, cipherText, authTag))

  def anonEncryptOKP(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeader,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Right(ECDH_AnonOKP(okpRecipientsKeys, header).encrypt(clearText))

  def anonDecryptOKP(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeader,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Right(ECDH_AnonOKP(okpRecipientsKeys, header).decrypt(recipients, iv, cipherText, authTag))

  def authEncryptOKP(
      sender: OKPKey,
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeader,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Right(ECDH_AuthOKP(sender, okpRecipientsKeys, header).encrypt(clearText))

  def authDecryptOKP(
      sender: OKPKey,
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeader,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Right(ECDH_AuthOKP(sender, okpRecipientsKeys, header).decrypt(recipients, iv, cipherText, authTag))
}
