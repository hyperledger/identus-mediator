package fmgp.crypto

import zio._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

trait CryptoOperations {

//sign
//verify
//anonEncrypt
//authEncrypt
//anonDecrypt
//authDecrypt

  def sign(
      key: PrivateKey,
      plaintext: PlaintextMessageClass
  ): IO[CryptoFailed, SignedMessage]

  def verify(
      key: OKP_EC_Key,
      jwm: SignedMessage
  ): IO[CryptoFailed, Boolean]

  // ###############
  // ### Encrypt ###
  // ###############

  def encrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = anonEncrypt(recipientKidsKeys, data: Array[Byte])

  def encrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): UIO[EncryptedMessageGeneric] = authEncrypt(senderKidKey, recipientKidsKeys, data)

  /** anoncrypt - Guarantees confidentiality and integrity without revealing the identity of the sender.
    */
  def anonEncrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): UIO[EncryptedMessageGeneric]

  /** authcrypt - Guarantees confidentiality and integrity. Also proves the identity of the sender – but in a way that
    * only the recipient can verify. This is the default wrapping choice, and SHOULD be used unless a different goal is
    * clearly identified. By design, this combination and all other combinations that use encryption in their outermost
    * layer share an identical IANA media type, because only the recipient should care about the difference.
    */
  def authEncrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): UIO[EncryptedMessageGeneric]

  // ###############
  // ### Decrypt ###
  // ###############

  def anonDecrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message]

  def authDecrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message]

  def anonDecryptOne(
      key: PrivateKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message]

  def authDecryptOne(
      recipientKey: PrivateKey,
      senderKey: PublicKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): IO[DidFail, Message]

}
