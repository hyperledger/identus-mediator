package fmgp.did.comm

import zio._
import zio.json._
import zio.http._
import zio.http.model._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.did.demo.MyHeaders

trait MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
      xForwardedHost: Option[String],
  ): ZIO[Any, DidFail, Unit]
}

object MessageDispatcher {
  val layer: ZLayer[Client, Throwable, MessageDispatcher] =
    ZLayer.fromZIO(
      ZIO
        .service[Client]
        .map(MyMessageDispatcher(_))
    )
}

class MyMessageDispatcher(client: Client) extends MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
      xForwardedHost: Option[String],
  ): ZIO[Any, DidFail, Unit] = {
    val contentTypeHeader = Headers.contentType(msg.`protected`.obj.typ.getOrElse(MediaTypes.ENCRYPTED).typ)
    val xForwardedHostHeader = Headers(xForwardedHost.map(x => Header(MyHeaders.xForwardedHost, x)))
    for {
      res <- Client
        .request(
          url = destination,
          method = Method.POST,
          headers = contentTypeHeader ++ xForwardedHostHeader,
          content = Body.fromCharSequence(msg.toJson),
        )
        .tapError(ex => ZIO.logWarning(s"Fail when calling '$destination': ${ex.toString}"))
        .mapError(ex => SomeThrowable(ex))
      data <- res.body.asString
        .tapError(ex => ZIO.logError(s"Fail parce http response body: ${ex.toString}"))
        .mapError(ex => SomeThrowable(ex))
      _ <- res.status.isError match
        case true  => ZIO.logError(data)
        case false => ZIO.logInfo(data)
    } yield ()
  }.provideEnvironment(ZEnvironment(client)) // .host()
}
