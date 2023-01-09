package fmgp.did.comm

import zio._
import zio.json._
import zio.http._
import zio.http.model._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

trait MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
  ): ZIO[Any, DidFail, Unit]
}

object MessageDispatcher {
  val layer: ZLayer[Any, Throwable, MessageDispatcher] = {
    val tmp = (for {
      client <- ZIO.service[Client]
      scope <- ZIO.service[Scope]
      m = MyMessageDispatcher(client)
    } yield m: MessageDispatcher).provide(Client.default, Scope.default)
    ZLayer.fromZIO(tmp)
  }
}

class MyMessageDispatcher(client: Client) extends MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
  ): ZIO[Any, DidFail, Unit] = {
    val contentTypeHeader = Header("content-type", msg.`protected`.obj.typ.getOrElse(MediaTypes.ENCRYPTED).typ)
    for {
      res <- Client
        .request(
          url = destination,
          method = Method.POST,
          headers = Headers(Seq(contentTypeHeader)),
          content = Body.fromCharSequence(msg.toJson),
        )
        .mapError(ex => SomeThrowable(ex))
      data <- res.body.asString.mapError(ex => SomeThrowable(ex))
      _ <- res.status.isError match
        case true  => ZIO.logError(data)
        case false => ZIO.logInfo(data)
    } yield ()
  }.provideEnvironment(ZEnvironment(client))
}
