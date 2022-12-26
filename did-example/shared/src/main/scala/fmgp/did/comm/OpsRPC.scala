package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._

sealed trait OpsRPC
@jsonDiscriminator("type") sealed trait OpsInputRPC extends OpsRPC
@jsonDiscriminator("type") sealed trait OpsOutputPRC extends OpsRPC
object OpsInputRPC {
  given decoder: JsonDecoder[OpsInputRPC] = DeriveJsonDecoder.gen[OpsInputRPC]
  given encoder: JsonEncoder[OpsInputRPC] = DeriveJsonEncoder.gen[OpsInputRPC]
}
object OpsOutputPRC {
  given decoder: JsonDecoder[OpsOutputPRC] = DeriveJsonDecoder.gen[OpsOutputPRC]
  given encoder: JsonEncoder[OpsOutputPRC] = DeriveJsonEncoder.gen[OpsOutputPRC]
}

case class AgentSimple(didSubject: DIDSubject, keys: Seq[PrivateKey]) extends Agent {
  def id: DID = didSubject
}
object AgentSimple {
  given JsonDecoder[AgentSimple] = DeriveJsonDecoder.gen[AgentSimple]
  given JsonEncoder[AgentSimple] = DeriveJsonEncoder.gen[AgentSimple]
}

case class SignOpInput(agent: AgentSimple, msg: PlaintextMessage) extends OpsInputRPC
case class SignOpOutput(ret: Either[CryptoFailed, SignedMessage]) extends OpsOutputPRC
case class VerifyOpInput( /*resolver: Resolver,*/ msg: SignedMessage) extends OpsInputRPC
case class VerifyOpOutput(ret: Either[CryptoFailed, Boolean]) extends OpsOutputPRC
case class AuthEncryptOpInput(agent: AgentSimple, /*resolver: Resolver,*/ msg: PlaintextMessage) extends OpsInputRPC
case class AuthEncryptOpOutput(ret: Either[DidFail, EncryptedMessage]) extends OpsOutputPRC
case class AuthDecryptOpInput(agent: AgentSimple, /*resolver: Resolver,*/ msg: EncryptedMessage) extends OpsInputRPC
case class AuthDecryptOpOutput(ret: Either[DidFail, Message]) extends OpsOutputPRC
case class AnonEncryptOpInput( /*resolver: Resolver,*/ msg: PlaintextMessage) extends OpsInputRPC
case class AnonEncryptOpOutput(ret: Either[DidFail, EncryptedMessage]) extends OpsOutputPRC
case class AnonDecryptOpInput(agent: AgentSimple, msg: EncryptedMessage) extends OpsInputRPC
case class AnonDecryptOpOutput(ret: Either[DidFail, Message]) extends OpsOutputPRC

object OpsRPC {
  // given JsonDecoder[SignOpInput] = DeriveJsonDecoder.gen[SignOpInput]
  // given JsonEncoder[SignOpInput] = DeriveJsonEncoder.gen[SignOpInput]
  // given JsonDecoder[SignOpOutput] = DeriveJsonDecoder.gen[SignOpOutput]
  // given JsonEncoder[SignOpOutput] = DeriveJsonEncoder.gen[SignOpOutput]

  // given JsonDecoder[VerifyOpInput] = DeriveJsonDecoder.gen[VerifyOpInput]
  // given JsonEncoder[VerifyOpInput] = DeriveJsonEncoder.gen[VerifyOpInput]
  // given JsonDecoder[VerifyOpOutput] = DeriveJsonDecoder.gen[VerifyOpOutput]
  // given JsonEncoder[VerifyOpOutput] = DeriveJsonEncoder.gen[VerifyOpOutput]

  // given JsonDecoder[AuthEncryptOpInput] = DeriveJsonDecoder.gen[AuthEncryptOpInput]
  // given JsonEncoder[AuthEncryptOpInput] = DeriveJsonEncoder.gen[AuthEncryptOpInput]
  // given JsonDecoder[AuthEncryptOpOutput] = DeriveJsonDecoder.gen[AuthEncryptOpOutput]
  // given JsonEncoder[AuthEncryptOpOutput] = DeriveJsonEncoder.gen[AuthEncryptOpOutput]

  // given JsonDecoder[AuthDecryptOpInput] = DeriveJsonDecoder.gen[AuthDecryptOpInput]
  // given JsonEncoder[AuthDecryptOpInput] = DeriveJsonEncoder.gen[AuthDecryptOpInput]
  // given JsonDecoder[AuthDecryptOpOutput] = DeriveJsonDecoder.gen[AuthDecryptOpOutput]
  // given JsonEncoder[AuthDecryptOpOutput] = DeriveJsonEncoder.gen[AuthDecryptOpOutput]

  // given JsonDecoder[AnonEncryptOpInput] = DeriveJsonDecoder.gen[AnonEncryptOpInput]
  // given JsonEncoder[AnonEncryptOpInput] = DeriveJsonEncoder.gen[AnonEncryptOpInput]
  // given JsonDecoder[AnonEncryptOpOutput] = DeriveJsonDecoder.gen[AnonEncryptOpOutput]
  // given JsonEncoder[AnonEncryptOpOutput] = DeriveJsonEncoder.gen[AnonEncryptOpOutput]

  // given JsonDecoder[AnonDecryptOpInput] = DeriveJsonDecoder.gen[AnonDecryptOpInput]
  // given JsonEncoder[AnonDecryptOpInput] = DeriveJsonEncoder.gen[AnonDecryptOpInput]
  // given JsonDecoder[AnonDecryptOpOutput] = DeriveJsonDecoder.gen[AnonDecryptOpOutput]
  // given JsonEncoder[AnonDecryptOpOutput] = DeriveJsonEncoder.gen[AnonDecryptOpOutput]

}
