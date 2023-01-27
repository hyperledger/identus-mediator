package fmgp.did.demo

import zio._
import zio.json._
import zio.stream._
import zio.http._
import zio.http.model._
import zio.http.socket._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._

import scala.io.Source
import fmgp.did.comm.mediator.MediatorAgent
import zio.http.ZClient.ClientLive

import laika.api._
import laika.format._
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting

/** demoJVM/runMain fmgp.did.demo.AppServer
  *
  * curl localhost:8080/hello
  *
  * curl 'http://localhost:8080/db' -H "host: alice.did.fmgp.app"
  *
  * wscat -c ws://localhost:8080 --host "alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json'
  *
  * curl -X POST localhost:8080 -H "host: alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json' -d
  * '{}'
  *
  * curl
  * localhost:8080/resolver/did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.Vz6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTA5My8iLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19
  */
object AppServer extends ZIOAppDefault {

  val mdocMarkdown = Http.collectHttp[Request] { case req @ Method.GET -> !! / "mdoc" / path =>
    Http.fromResource(s"$path")
  }

  val mdocHTML = Http.collectHttp[Request] { case req @ Method.GET -> !! / "doc" / path =>
    val transformer = Transformer
      .from(Markdown)
      .to(HTML)
      .using(GitHubFlavor, SyntaxHighlighting)
      .build

    Http.fromResource(s"$path").mapZIO {
      _.body.asString.map { data =>
        val result = transformer.transform(data) match
          case Left(value)  => value.message
          case Right(value) => value
        Response.html(result)
      }
    }
  }

  val app: Http[
    Hub[String] & AgentByHost & Operations & MessageDispatcher,
    Throwable,
    Request,
    Response
  ] = MediatorAgent.didCommApp ++ Http
    .collectZIO[Request] {
      case Method.GET -> !! / "hello" => ZIO.succeed(Response.text("Hello World! DEMO DID APP")).debug
      // http://localhost:8080/oob?_oob=eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOnZlcmlmaWVyIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ
      case req @ Method.GET -> !! / "oob" =>
        ZIO.succeed(OutOfBand.oob(req.url.encode) match
          case Left(error)                          => Response.text(error).copy(status = Status.BadRequest)
          case Right(OutOfBandPlaintext(msg, data)) => Response.json(msg.toJsonPretty)
          case Right(OutOfBandSigned(msg, data))    => Response.json(msg.payload.content)
        )
      case req @ Method.GET -> !! / "db" =>
        for {
          agent <- AgentByHost.getAgentFor(req)
          db <- agent.messageDB.get
          ret <- ZIO.succeed(Response.json(db.toJsonPretty))
        } yield (ret)
      case req @ Method.GET -> !! / "socket" =>
        for {
          agent <- AgentByHost.getAgentFor(req)
          sm <- agent.didSocketManager.get
          ret <- ZIO.succeed(Response.text(sm.toJsonPretty))
        } yield (ret)
      case req @ Method.POST -> !! / "socket" / id =>
        for {
          hub <- ZIO.service[Hub[String]]
          agent <- AgentByHost.getAgentFor(req)
          sm <- agent.didSocketManager.get
          ret <- sm.ids
            .get(FROMTO(id))
            .toSeq
            .flatMap { socketsID =>
              socketsID.flatMap(id => sm.sockets.get(id).map(e => (id, e))).toSeq
            } match {
            case Seq() =>
              req.body.asString.flatMap(e => hub.publish(s"socket missing for $id"))
                *> ZIO.succeed(Response.text(s"socket missing"))
            case seq =>
              ZIO.foreach(seq) { (socketID, channel) =>
                req.body.asString.flatMap(e => channel.socketOutHub.publish(e))
              } *> ZIO.succeed(Response.text(s"message sended"))
          }
        } yield (ret)
      case req @ Method.GET -> !! / "headers" =>
        val data = req.headersAsList.toSeq.map(e => (e.key.toString(), e.value.toString()))
        ZIO.succeed(Response.text("HEADERS:\n" + data.mkString("\n"))).debug
      case req @ Method.POST -> !! / "ops" =>
        req.body.asString
          .flatMap(e => OperationsServerRPC.ops(e))
          .map(e => Response.text(e))
      case Method.GET -> !! / "resolver" / did =>
        DIDSubject.either(did) match
          case Left(error)  => ZIO.succeed(Response.text(error.error).setStatus(Status.BadRequest)).debug
          case Right(value) => ZIO.succeed(Response.text("DID:" + value)).debug
      case req @ Method.GET -> !! => { // html.Html.fromDomElement()
        val data = Source.fromResource(s"public/index.html").mkString("")
        ZIO.succeed(Response.html(data))
      }
    }
    ++ {
      Http.fromResource(s"public/fmgp-webapp-fastopt-bundle.js").when {
        case Method.GET -> !! / "public" / path => true
        // Response(
        //   body = Body.fromStream(ZStream.fromIterator(Source.fromResource(s"public/$path").iter).map(_.toByte)),
        //   headers = Headers(HeaderNames.contentType, HeaderValues.applicationJson),
        // )
        case _ => false
      }
    }
    ++ mdocMarkdown ++ mdocHTML

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
    myHub <- Hub.sliding[String](5)
    _ <- ZStream.fromHub(myHub).run(ZSink.foreach((str: String) => Console.printLine("HUB: " + str))).fork
    pord <- System
      .property("PORD")
      .flatMap {
        case None        => System.property("pord")
        case Some(value) => ZIO.succeed(Some(value))
      }
      .map(_.flatMap(_.toBooleanOption).getOrElse(false))
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
    client = Scope.default >>> Client.default
    inboundHub <- Hub.bounded[String](5)
    myServer <- Server
      .serve(
        app.foldCauseHttp( // THIS is to log all the erros
          cause => Http.fromZIO(ZIO.logErrorCause(cause)) *> Http.failCause(cause),
          Http.succeed,
          Http.empty
        )
      )
      .provideSomeEnvironment { (env: ZEnvironment[Server & AgentByHost & Operations & MessageDispatcher]) =>
        env.add(myHub)
      }
      .provideSomeLayer(AgentByHost.layer)
      .provideSomeLayer(MyOperations.layer)
      .provideSomeLayer(client >>> MessageDispatcher.layer)
      .provide(server)
      .debug
      .fork
    _ <- Console.printLine(s"Server Started")
    _ <- myServer.join *> Console.printLine(s"Server End")
  } yield ()

}
