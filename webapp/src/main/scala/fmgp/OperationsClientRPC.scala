package fmgp.did.comm

import scala.scalajs.js
import org.scalajs.dom._

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.Resolver
import fmgp.crypto._
import fmgp.crypto.error._

object OperationsClientRPC extends Operations {

  override def sign(msg: PlaintextMessage): ZIO[Agent, CryptoFailed, SignedMessage] = for {
    agent <- ZIO.service[Agent]
    obj = SignOpInput(AgentSimple(agent.id, agent.keys), msg): OpsInputRPC
    ret <- Client.makeOps(data = obj.toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[SignOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(SignOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(SignOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                       => ZIO.fail(value)
    }
  } yield (out)

  override def verify(msg: SignedMessage): ZIO[Resolver, CryptoFailed, Boolean] = for {
    ret <- Client.makeOps(data = (VerifyOpInput(msg): OpsInputRPC).toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[VerifyOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(VerifyOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(VerifyOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                         => ZIO.fail(value)
    }
  } yield (out)

  override def authEncrypt(msg: PlaintextMessage): ZIO[Agent & Resolver, DidFail, EncryptedMessage] = for {
    agent <- ZIO.service[Agent]
    // resolver <- ZIO.service[Resolver]
    obj = AuthEncryptOpInput(AgentSimple(agent.id, agent.keys), msg): OpsInputRPC
    ret <- Client.makeOps(data = obj.toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[AuthEncryptOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(AuthEncryptOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(AuthEncryptOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                              => ZIO.fail(value)
    }
  } yield (out)

  override def authDecryptRaw(msg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, Array[Byte]] = for {
    agent <- ZIO.service[Agent]
    // resolver <- ZIO.service[Resolver]
    obj = AuthDecryptRawOpInput(AgentSimple(agent.id, agent.keys), msg): OpsInputRPC
    ret <- Client.makeOps(data = obj.toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[AuthDecryptRawOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(AuthDecryptRawOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(AuthDecryptRawOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                                 => ZIO.fail(value)
    }
  } yield (out)

  override def anonEncrypt(msg: PlaintextMessage): ZIO[Resolver, DidFail, EncryptedMessage] = for {
    // agent <- ZIO.service[Agent]
    // resolver <- ZIO.service[Resolver]
    ret <- Client.makeOps(data = (AnonEncryptOpInput(msg): OpsInputRPC).toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[AnonEncryptOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(AnonEncryptOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(AnonEncryptOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                              => ZIO.fail(value)
    }
  } yield (out)

  override def anonDecryptRaw(msg: EncryptedMessage): ZIO[Agent, DidFail, Array[Byte]] = for {
    agent <- ZIO.service[Agent]
    // resolver <- ZIO.service[Resolver]
    obj = AnonDecryptRawOpInput(AgentSimple(agent.id, agent.keys), msg): OpsInputRPC
    ret <- Client.makeOps(data = obj.toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[AnonDecryptRawOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(AnonDecryptRawOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(AnonDecryptRawOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                                 => ZIO.fail(value)
    }
  } yield (out)

}
