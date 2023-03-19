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
  import DidFail.decoder
  import CryptoFailed.decoder
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
// case class AuthDecryptOpInput(agent: AgentSimple, /*resolver: Resolver,*/ msg: EncryptedMessage) extends OpsInputRPC
//case class AuthDecryptOpOutput(ret: Either[DidFail, Message]) extends OpsOutputPRC
case class AuthDecryptRawOpInput(agent: AgentSimple, /*resolver: Resolver,*/ msg: EncryptedMessage) extends OpsInputRPC
case class AuthDecryptRawOpOutput(ret: Either[DidFail, Array[Byte]]) extends OpsOutputPRC
case class AnonEncryptOpInput( /*resolver: Resolver,*/ msg: PlaintextMessage) extends OpsInputRPC
case class AnonEncryptOpOutput(ret: Either[DidFail, EncryptedMessage]) extends OpsOutputPRC
// case class AnonDecryptOpInput(agent: AgentSimple, msg: EncryptedMessage) extends OpsInputRPC
// case class AnonDecryptOpOutput(ret: Either[DidFail, Message]) extends OpsOutputPRC
case class AnonDecryptRawOpInput(agent: AgentSimple, msg: EncryptedMessage) extends OpsInputRPC
case class AnonDecryptRawOpOutput(ret: Either[DidFail, Array[Byte]]) extends OpsOutputPRC
