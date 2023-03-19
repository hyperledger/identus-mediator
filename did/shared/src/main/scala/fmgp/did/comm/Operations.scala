package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto.error._
import fmgp.crypto._

/** DID Comm operations */
trait Operations {

  def sign(msg: PlaintextMessage): ZIO[Agent, CryptoFailed, SignedMessage]

  def verify(msg: SignedMessage): ZIO[Resolver, CryptoFailed, Boolean] // SignatureVerificationFailed.type

  def anonEncrypt(msg: PlaintextMessage): ZIO[Resolver, DidFail, EncryptedMessage]

  def authEncrypt(msg: PlaintextMessage): ZIO[Agent & Resolver, DidFail, EncryptedMessage]

  def anonDecryptRaw(msg: EncryptedMessage): ZIO[Agent, DidFail, Array[Byte]]
  def authDecryptRaw(msg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, Array[Byte]]

  /** decrypt */
  def anonDecrypt(msg: EncryptedMessage): ZIO[Agent, DidFail, Message] =
    anonDecryptRaw(msg).flatMap(data =>
      ZIO.fromEither {
        String(data)
          .fromJson[Message]
          .left
          .map(info => FailToParse(s"Decoding into a Message: $info"))
      }
    )

  /** decrypt verify sender */
  def authDecrypt(msg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, Message] =
    authDecryptRaw(msg).flatMap(data =>
      ZIO.fromEither {
        String(data)
          .fromJson[Message]
          .left
          .map(info => FailToParse(s"Decoding into a Message: $info"))
      }
    )

  def verify2PlaintextMessage(
      msg: SignedMessage
  ): ZIO[Operations & Resolver, CryptoFailed, PlaintextMessage] = for {
    payload <- verify(msg).flatMap {
      case false => ZIO.fail(SignatureVerificationFailed)
      case true  => ZIO.succeed(msg.payload)
    }
    plaintextMessage <- payload.content.fromJson[PlaintextMessage] match
      case Left(error)  => ZIO.fail(CryptoFailToParse(error))
      case Right(value) => ZIO.succeed(value)
  } yield plaintextMessage
}

object Operations {

  def sign(
      msg: PlaintextMessage
  ): ZIO[Operations & Agent, CryptoFailed, SignedMessage] =
    ZIO.serviceWithZIO[Operations](_.sign(msg))

  def verify(
      msg: SignedMessage
  ): ZIO[Operations & Resolver, CryptoFailed, Boolean] =
    ZIO.serviceWithZIO[Operations](_.verify(msg))

  def anonEncrypt(
      msg: PlaintextMessage
  ): ZIO[Operations & Resolver, DidFail, EncryptedMessage] =
    ZIO.serviceWithZIO[Operations](_.anonEncrypt(msg))

  def authEncrypt(
      msg: PlaintextMessage,
  ): ZIO[Operations & Agent & Resolver, DidFail, EncryptedMessage] =
    ZIO.serviceWithZIO[Operations](_.authEncrypt(msg))

  /** decrypt */
  def anonDecrypt(
      msg: EncryptedMessage
  ): ZIO[Operations & Agent, DidFail, Message] =
    ZIO.serviceWithZIO[Operations](_.anonDecrypt(msg))

  /** decryptAndVerify */
  def authDecrypt(
      msg: EncryptedMessage
  ): ZIO[Operations & Agent & Resolver, DidFail, Message] =
    ZIO.serviceWithZIO[Operations](_.authDecrypt(msg))

  def metaData(msg: EncryptedMessage) = msg.`protected`.obj match
    case AnonProtectedHeader(epk, apv, typ, enc, alg)            =>
    case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>

}
