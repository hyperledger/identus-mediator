package io.iohk.atala.mediator.comm

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import io.iohk.atala.mediator.comm.*
import io.iohk.atala.mediator.utils.MyHeaders
import zio.*
import zio.http.*
import zio.http.model.*
import zio.json.*

object MessageDispatcherJVM {
  val layer: ZLayer[Client, Throwable, MessageDispatcher] =
    ZLayer.fromZIO(
      ZIO
        .service[Client]
        .map(MessageDispatcherJVM(_))
    )
}

class MessageDispatcherJVM(client: Client) extends MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
      xForwardedHost: Option[String],
  ): ZIO[Any, DidFail, String] = {
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
    } yield (data)
  }.provideEnvironment(ZEnvironment(client)) // .host()
}
