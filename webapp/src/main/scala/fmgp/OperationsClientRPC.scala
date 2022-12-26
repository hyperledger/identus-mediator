package fmgp.did.comm

import fmgp.crypto.error.DidFail

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._

import zio._
import zio.json._

import fmgp.did.Resolver
import typings.materialBase.materialBaseStrings.input

import scala.scalajs.js
import org.scalajs.dom._
import org.scalajs.dom

object OperationsClientRPC extends Operations {

  def makeOps(
      data: String,
      url: String = "https://did.fmgp.app/ops" // "http://localhost:8080/ops" // FIXME url
  ): IO[SomeThrowable, String] = ZIO
    .fromPromiseJS(
      dom.fetch(url, new RequestInit { method = HttpMethod.POST; body = data })
    )
    .flatMap(e => ZIO.fromPromiseJS(e.text()))
    .catchAll(ex => ZIO.fail(SomeThrowable(ex)))

  override def sign(msg: PlaintextMessage): ZIO[Agent, CryptoFailed, SignedMessage] = for {
    agent <- ZIO.service[Agent]
    obj = SignOpInput(AgentSimple(agent.id, agent.keys), msg): OpsInputRPC
    ret <- makeOps(data = obj.toJson)
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
    ret <- makeOps(data = (VerifyOpInput(msg): OpsInputRPC).toJson)
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
    ret <- makeOps(data = obj.toJson)
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

  override def authDecrypt(msg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, Message] = for {
    agent <- ZIO.service[Agent]
    // resolver <- ZIO.service[Resolver]
    obj = AuthDecryptOpInput(AgentSimple(agent.id, agent.keys), msg): OpsInputRPC
    ret <- makeOps(data = obj.toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[AuthDecryptOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(AuthDecryptOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(AuthDecryptOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                              => ZIO.fail(value)
    }
  } yield (out)

  override def anonEncrypt(msg: PlaintextMessage): ZIO[Resolver, DidFail, EncryptedMessage] = for {
    // agent <- ZIO.service[Agent]
    // resolver <- ZIO.service[Resolver]
    ret <- makeOps(data = (AnonEncryptOpInput(msg): OpsInputRPC).toJson)
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

  override def anonDecrypt(msg: EncryptedMessage): ZIO[Agent, DidFail, Message] = for {
    agent <- ZIO.service[Agent]
    // resolver <- ZIO.service[Resolver]
    obj = AnonDecryptOpInput(AgentSimple(agent.id, agent.keys), msg): OpsInputRPC
    ret <- makeOps(data = obj.toJson)
    out <- OpsOutputPRC.decoder
      .decodeJson(ret)
      .map(_.asInstanceOf[AnonDecryptOpOutput])
      .left
      .map(ex => CryptoFailToParse(ex)) match {
      case Right(AnonDecryptOpOutput(Right(value))) => ZIO.succeed(value)
      case Right(AnonDecryptOpOutput(Left(value)))  => ZIO.fail(value)
      case Left(value)                              => ZIO.fail(value)
    }
  } yield (out)

}
