package fmgp.did.demo

import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import zio._
import zio.json._
import fmgp.did.resolver.peer._

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
            case AuthDecryptOpInput(agent, msg) =>
              operations
                .authDecrypt(msg)
                .provideEnvironment(ZEnvironment(agent, resolver))
                .mapBoth(ex => AuthDecryptOpOutput(Left(ex)), e => AuthDecryptOpOutput(Right(e)))
                .merge
            case AnonEncryptOpInput(msg) =>
              operations
                .anonEncrypt(msg)
                .provideEnvironment(ZEnvironment(resolver))
                .mapBoth(ex => AnonEncryptOpOutput(Left(ex)), e => AnonEncryptOpOutput(Right(e)))
                .merge
            case AnonDecryptOpInput(agent, msg) =>
              operations
                .anonDecrypt(msg)
                .provideEnvironment(ZEnvironment(agent))
                .mapBoth(ex => AnonDecryptOpOutput(Left(ex)), e => AnonDecryptOpOutput(Right(e)))
                .merge
          }
        } yield (result.toJson)
        tmp
          .provideSomeLayer(MyOperations.layer)
          .provideSomeLayer(ZLayer.succeed(DidPeerResolver))
    }

}
