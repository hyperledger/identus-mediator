package fmgp.did.demo

import zio._
import zio.json._
import zio.stream._
import zio.http._
import zio.http.model._
import zio.http.socket._
import zio.http.ZClient.ClientLive
import zio.http.Http.Empty
import zio.http.Http.Static

import scala.io.Source

import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.util._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.mediator._
import fmgp.did.comm.protocol._
import fmgp.did.resolver.peer.DidPeerResolver

object MediatorStandalone extends ZIOAppDefault {

  val app: HttpApp[ // type HttpApp[-R, +Err] = Http[R, Err, Request, Response]
    Hub[String] & Operations & MessageDispatcher & MediatorAgent & Ref[MediatorDB] & Resolver,
    Throwable
  ] = MediatorAgent.didCommApp
    ++ Http
      .collectZIO[Request] { case Method.GET -> !! / "hello" =>
        ZIO.succeed(Response.text("Hello World! DID Comm Mediator APP")).debug
      }

  val mediatorAgentLayer = ZLayer(
    MediatorAgent.make( // https://mediator.did.fmgp.app/
      DIDSubject(
        "did:peer:2"
          + ".Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"
          + ".Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd"
          + ".SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9tZWRpYXRvci5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
      ),
      KeyStore(
        Set(
          OKPPrivateKey(
            kty = KTY.OKP,
            crv = Curve.X25519,
            d = "Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c",
            x = "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
            kid = None
          ), // keyAgreement
          OKPPrivateKey(
            kty = KTY.OKP,
            crv = Curve.Ed25519,
            d = "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
            x = "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
            kid = None
          ) // keyAuthentication
        )
      ),
    )
  )

  override val run = for {
    _ <- Console.printLine( // https://patorjk.com/software/taag/#p=display&f=ANSI%20Shadow&t=Mediator
      """███╗   ███╗███████╗██████╗ ██╗ █████╗ ████████╗ ██████╗ ██████╗ 
        |████╗ ████║██╔════╝██╔══██╗██║██╔══██╗╚══██╔══╝██╔═══██╗██╔══██╗
        |██╔████╔██║█████╗  ██║  ██║██║███████║   ██║   ██║   ██║██████╔╝
        |██║╚██╔╝██║██╔══╝  ██║  ██║██║██╔══██║   ██║   ██║   ██║██╔══██╗
        |██║ ╚═╝ ██║███████╗██████╔╝██║██║  ██║   ██║   ╚██████╔╝██║  ██║
        |╚═╝     ╚═╝╚══════╝╚═════╝ ╚═╝╚═╝  ╚═╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝
        |Yet another server simpler Mediator server DID Comm v2.
        |Vist: https://github.com/FabioPinheiro/scala-did""".stripMargin
    )
    _ <- ZIO.log(s"DID DEMO APP. See https://github.com/FabioPinheiro/scala-did")
    myHub <- Hub.sliding[String](5)
    _ <- ZStream.fromHub(myHub).run(ZSink.foreach((str: String) => ZIO.logInfo("HUB: " + str))).fork
    // pord <- System
    //   .property("PORD")
    //   .flatMap {
    //     case None        => System.property("pord")
    //     case Some(value) => ZIO.succeed(Some(value))
    //   }
    //   .map(_.flatMap(_.toBooleanOption).getOrElse(false))
    port <- System
      .property("PORT")
      .flatMap {
        case None        => System.property("port")
        case Some(value) => ZIO.succeed(Some(value))
      }
      .map(_.flatMap(_.toIntOption).getOrElse(8080))
    _ <- ZIO.log(s"Starting server on port: $port")
    server = {
      val config = ServerConfig(address = new java.net.InetSocketAddress(port))
      ServerConfig.live(config)(using Trace.empty) >>> Server.live
    }
    client = Scope.default >>> Client.default
    inboundHub <- Hub.bounded[String](5)
    myServer <- Server
      .serve(
        app.annotateLogs
          .tapUnhandledZIO(ZIO.logError("Unhandled Endpoint"))
          .tapErrorCauseZIO(cause => ZIO.logErrorCause(cause)) // THIS is to log all the erros
          .mapError(err =>
            Response(
              status = Status.BadRequest,
              headers = Headers.empty,
              body = Body.fromString(err.getMessage()),
            )
          )
      )
      .provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
      .provideSomeLayer(mediatorAgentLayer) // .provideSomeLayer(AgentByHost.layer)
      .provideSomeLayer(Operations.layerDefault)
      .provideSomeLayer(client >>> MessageDispatcherJVM.layer)
      .provideSomeLayer(ZLayer.fromZIO(Ref.make[MediatorDB](MediatorDB.empty))) // TODO move into AgentByHost
      .provideSomeEnvironment { (env: ZEnvironment[Server]) => env.add(myHub) }
      .provide(server)
      .debug
      .fork
    _ <- ZIO.log(s"Mediator Started")
    _ <- myServer.join *> ZIO.log(s"Mediator End")
  } yield ()

}
