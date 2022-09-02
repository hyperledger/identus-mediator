package fmgp.did.comm

import zio._

import fmgp.did._
import fmgp.crypto.error._

/** DID Comm operations */
trait Operations {

  def sign(msg: PlaintextMessageClass): URIO[Agent, SignedMessage]

  def verify(msg: PlaintextMessageClass): ZIO[Resolver, SignatureVerificationFailed.type, Unit]

  def anonEncrypt(msg: PlaintextMessageClass): ZIO[Resolver, EncryptionFailed.type, EncryptedMessage]

  def authEncrypt(msg: PlaintextMessageClass): ZIO[Agent & Resolver, EncryptionFailed.type, EncryptedMessage]

  /** decrypt */
  def anonDecrypt(msg: EncryptedMessage): ZIO[Agent & Resolver, CryptoFailed, Message]

  /** decrypt verify sender */
  def authDecrypt(msg: EncryptedMessage): ZIO[Agent & Resolver, CryptoFailed, Message]

}

object Operations {

  def sign(
      msg: PlaintextMessageClass
  ): URIO[Operations & Agent, SignedMessage] =
    ZIO.serviceWithZIO[Operations](_.sign(msg))

  def verify(
      msg: PlaintextMessageClass
  ): ZIO[Operations & Resolver, SignatureVerificationFailed.type, Unit] =
    ZIO.serviceWithZIO[Operations](_.verify(msg))

  def anonEncrypt(
      msg: PlaintextMessageClass
  ): ZIO[Operations & Resolver, EncryptionFailed.type, EncryptedMessage] =
    ZIO.serviceWithZIO[Operations](_.anonEncrypt(msg))

  def authEncrypt(
      msg: PlaintextMessageClass,
  ): ZIO[Operations & Agent & Resolver, EncryptionFailed.type, EncryptedMessage] =
    ZIO.serviceWithZIO[Operations](_.authEncrypt(msg))

  /** decrypt */
  def anonDecrypt(
      msg: EncryptedMessage
  ): ZIO[Operations & Agent & Resolver, CryptoFailed, Message] =
    ZIO.serviceWithZIO[Operations](_.anonDecrypt(msg))

  /** decryptAndVerify */
  def authDecrypt(
      msg: EncryptedMessage
  ): ZIO[Operations & Agent & Resolver, CryptoFailed, Message] =
    ZIO.serviceWithZIO[Operations](_.authDecrypt(msg))

}
