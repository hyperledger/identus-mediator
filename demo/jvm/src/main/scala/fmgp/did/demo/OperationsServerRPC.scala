package fmgp.did.demo

import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import zio._
import zio.json._
import fmgp.did.resolver.peer._
import fmgp.did.resolver.hardcode.HardcodeResolver
import fmgp.did.resolver.uniresolver.Uniresolver

object OperationsServerRPC {

  def ops(input: String): ZIO[Any, DidException, String] =
    input.fromJson[OpsInputRPC] match {
      case Left(error) => ZIO.fail(DidException(FailToParse(error)))
      case Right(value) =>
        val tmp = for {
          operations <- ZIO.service[Operations]
          resolver <- ZIO.service[Resolver]
          result <- value match {
            case SignOpInput(agent, msg) =>
              operations
                .sign(msg)
                .provideEnvironment(ZEnvironment(agent))
                .mapBoth(ex => SignOpOutput(Left(ex)), e => SignOpOutput(Right(e)))
                .merge
            case VerifyOpInput(msg) =>
              operations
                .verify(msg)
                .mapBoth(ex => VerifyOpOutput(Left(ex)), e => VerifyOpOutput(Right(e)))
                .merge
            case AuthEncryptOpInput(agent, msg) =>
              operations
                .authEncrypt(msg)
                .provideEnvironment(ZEnvironment(agent, resolver))
                .mapBoth(ex => AuthEncryptOpOutput(Left(ex)), e => AuthEncryptOpOutput(Right(e)))
                .merge
            case AuthDecryptRawOpInput(agent, msg) =>
              operations
                .authDecryptRaw(msg)
                .provideEnvironment(ZEnvironment(agent, resolver))
                .mapBoth(ex => AuthDecryptRawOpOutput(Left(ex)), e => AuthDecryptRawOpOutput(Right(e)))
                .merge
            case AnonEncryptOpInput(msg) =>
              operations
                .anonEncrypt(msg)
                .provideEnvironment(ZEnvironment(resolver))
                .mapBoth(ex => AnonEncryptOpOutput(Left(ex)), e => AnonEncryptOpOutput(Right(e)))
                .merge
            case AnonDecryptRawOpInput(agent, msg) =>
              operations
                .anonDecryptRaw(msg)
                .provideEnvironment(ZEnvironment(agent))
                .mapBoth(ex => AnonDecryptRawOpOutput(Left(ex)), e => AnonDecryptRawOpOutput(Right(e)))
                .merge
          }
        } yield (result.toJson)
        tmp
          .provideSomeLayer(Operations.layerDefault)
          .provideSomeLayer(
            ZLayer.succeed(
              MultiResolver(
                HardcodeResolver.default,
                Uniresolver.default,
                DidPeerResolver.default,
              )
            )
          )
    }

}
