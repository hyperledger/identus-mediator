package io.iohk.atala.mediator.comm

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import io.iohk.atala.mediator.comm.*
import io.iohk.atala.mediator.utils.MyHeaders
import zio.*
import zio.http.{MediaType => ZMediaType, *}
import zio.json.*

import scala.util.chaining._

trait MessageDispatcherIOHK {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
      xForwardedHost: Option[String],
  ): ZIO[Any, DispatcherError, String]
}

object MessageDispatcherJVMIOHK {
  val layer: ZLayer[Client, Throwable, MessageDispatcherIOHK] =
    ZLayer.fromZIO(
      ZIO
        .service[Client]
        .map(MessageDispatcherJVMIOHK(_))
    )
}

class MessageDispatcherJVMIOHK(client: Client) extends MessageDispatcherIOHK {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
      xForwardedHost: Option[String],
  ): ZIO[Any, DispatcherError, String] = {
    val contentTypeHeader = msg.`protected`.obj.typ
      .getOrElse(MediaTypes.ENCRYPTED)
      // .pipe(e => Header.ContentType(ZMediaType(e.mainType, e.subType))) FIXME
      .pipe(e => Header.ContentType(ZMediaType.application.any.copy(subType = "didcomm-encrypted+json")))
    val xForwardedHostHeader = xForwardedHost.map(x => Header.Custom(customName = MyHeaders.xForwardedHost, x))

    for {
      res <- Client
        .request(
          Request
            .post(destination, Body.fromCharSequence(msg.toJson))
            .setHeaders(Headers(Seq(Some(contentTypeHeader), xForwardedHostHeader).flatten))
        )
        .tapError(ex => ZIO.logWarning(s"Fail when calling '$destination': ${ex.toString}"))
        .mapError(ex => DispatcherError(ex))
      data <- res.body.asString
        .tapError(ex => ZIO.logError(s"Fail parce http response body: ${ex.toString}"))
        .mapError(ex => DispatcherError(ex))
      _ <- res.status.isError match
        case true  => ZIO.logWarning(data)
        case false => ZIO.logInfo(data)
    } yield (data)
  }
    .provideSomeLayer(Scope.default)
    .provideEnvironment(ZEnvironment(client))
}
