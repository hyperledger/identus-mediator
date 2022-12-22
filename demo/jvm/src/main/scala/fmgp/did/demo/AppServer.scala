package fmgp.did.demo

import zio._
import zio.json._
import zio.http._
import zio.http.model._
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}

import fmgp.did._
import fmgp.crypto.error._
import fmgp.did.comm._

import scala.io.Source

/** demoJVM/runMain fmgp.did.demo.AppServer
  *
  * curl localhost:8080/hello
  *
  * wscat -c ws://localhost:8080/ws
  *
  * curl -X POST localhost:8080 -H 'content-type: application/didcomm-encrypted+json' -d '{}'
  *
  * curl
  * localhost:8080/resolver/did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.Vz6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTA5My8iLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19
  */
object AppServer extends ZIOAppDefault {

  private val socket: Http[Any, Throwable, WebSocketChannelEvent, Unit] =
    Http.collectZIO[WebSocketChannelEvent] {
      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text("FOO"))) =>
        ch.writeAndFlush(WebSocketFrame.text("BAR"))

      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text("BAR"))) =>
        ch.writeAndFlush(WebSocketFrame.text("FOO"))

      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
        ch.write(WebSocketFrame.text("ws:" + text)).repeatN(0) *> ch.flush
    }

  val app: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case Method.GET -> !! / "hello" => ZIO.succeed(Response.text("Hello World! DEMO DID APP"))
    case req @ Method.GET -> !! / "headers" =>
      val data = req.headersAsList.toSeq.map(e => (e.key.toString(), e.value.toString()))
      ZIO.succeed(Response.text("HEADERS:\n" + data.mkString("\n")))
    case Method.GET -> !! / "ws" => socket.toSocketApp.toResponse
    case req @ Method.POST -> !! if req.headersAsList.exists { h =>
          h.key == "content-type" &&
          (h.value == MediaTypes.SIGNED || h.value == MediaTypes.ENCRYPTED.typ)
        } =>
      for {
        data <- req.body.asString
        msg <- ZIO.fromEither(
          data
            .fromJson[Message]
            .left
            .map(error => DidException(FailToParse(error)))
        )
        _ <- ZIO.log(msg.toJsonPretty)
      } yield Response.text(msg.toJson)

    case Method.POST -> !! =>
      ZIO.succeed(Response.text(s"The content-type must be ${MediaTypes.SIGNED.typ} and ${MediaTypes.ENCRYPTED.typ}"))
    case Method.GET -> !! / "resolver" / did =>
      DIDSubject.either(did) match
        case Left(error)  => ZIO.succeed(Response.text(error.error).setStatus(Status.BadRequest))
        case Right(value) => ZIO.succeed(Response.text("DID:" + value))
    case req @ Method.GET -> !! => {
      val data = Source.fromResource(s"public/index.html").mkString("")
      ZIO.succeed(Response.html(data))
    }
    case Method.GET -> !! / "public" / path => {
      ZIO.succeed(
        Response(
          body = Body.fromString(Source.fromResource(s"public/$path").getLines.mkString("\n")),
          headers = Headers(HeaderNames.contentType, HeaderValues.applicationJson),
        )
      )
    }

  }
  override val run = for {
    _ <- Console.printLine(
      """██████╗ ██╗██████╗     ██████╗ ███████╗███╗   ███╗ ██████╗ 
        |██╔══██╗██║██╔══██╗    ██╔══██╗██╔════╝████╗ ████║██╔═══██╗
        |██║  ██║██║██║  ██║    ██║  ██║█████╗  ██╔████╔██║██║   ██║
        |██║  ██║██║██║  ██║    ██║  ██║██╔══╝  ██║╚██╔╝██║██║   ██║
        |██████╔╝██║██████╔╝    ██████╔╝███████╗██║ ╚═╝ ██║╚██████╔╝
        |╚═════╝ ╚═╝╚═════╝     ╚═════╝ ╚══════╝╚═╝     ╚═╝ ╚═════╝ 
        |Yet another server simpler server to demo DID Comm v2.
        |Vist: https://github.com/FabioPinheiro/scala-did""".stripMargin
    )
    port <- System
      .property("PORT")
      .flatMap {
        case None        => System.property("port")
        case Some(value) => ZIO.succeed(Some(value))
      }
      .map(_.flatMap(_.toIntOption).getOrElse(8080))

    _ <- Console.printLine(s"Starting server on port: $port")
    server = {
      val config = ServerConfig(address = new java.net.InetSocketAddress(port))
      ServerConfig.live(config)(using Trace.empty) >>> Server.live
    }
    _ <- Server.serve(app).provide(server)
  } yield ()

}
