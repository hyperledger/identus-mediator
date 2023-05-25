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
import zio.config._
import zio.config.magnolia._
import zio.config.typesafe._

import scala.io.Source

import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.util._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.mediator._
import fmgp.did.comm.protocol._
import fmgp.did.method.peer._

case class MediatorConfig(endpoint: java.net.URI, keyAgreement: OKPPrivateKey, keyAuthentication: OKPPrivateKey) {
  val did = DIDPeer2.makeAgent(
    Seq(keyAgreement, keyAuthentication),
    Seq(DIDPeerServiceEncoded(s = endpoint.toString()))
  )
  val agentLayer = ZLayer(MediatorAgent.make(id = did.id, keyStore = did.keyStore))
}

object MediatorStandalone extends ZIOAppDefault {

  val app: HttpApp[ // type HttpApp[-R, +Err] = Http[R, Err, Request, Response]
    Hub[String] & Operations & MessageDispatcher & MediatorAgent & Ref[MediatorDB] & Resolver,
    Throwable
  ] = MediatorAgent.didCommApp
    ++ Http
      .collectZIO[Request] { case Method.GET -> !! / "hello" =>
        ZIO.succeed(Response.text("Hello World! DID Comm Mediator APP")).debug
      }
  override val run = for {
    _ <- Console.printLine( // https://patorjk.com/software/taag/#p=display&f=ANSI%20Shadow&t=Mediator
      """███╗   ███╗███████╗██████╗ ██╗ █████╗ ████████╗ ██████╗ ██████╗ 
        |████╗ ████║██╔════╝██╔══██╗██║██╔══██╗╚══██╔══╝██╔═══██╗██╔══██╗
        |██╔████╔██║█████╗  ██║  ██║██║███████║   ██║   ██║   ██║██████╔╝
        |██║╚██╔╝██║██╔══╝  ██║  ██║██║██╔══██║   ██║   ██║   ██║██╔══██╗
        |██║ ╚═╝ ██║███████╗██████╔╝██║██║  ██║   ██║   ╚██████╔╝██║  ██║
        |╚═╝     ╚═╝╚══════╝╚═════╝ ╚═╝╚═╝  ╚═╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝
        |Yet another server simpler Mediator server DID Comm v2.
        |Vist: https://github.com/input-output-hk/atala-prism-mediator""".stripMargin
    )
    configs = ConfigProvider.fromResourcePath()
    mediatorConfig <- configs.nested("identity").nested("mediator").load(deriveConfig[MediatorConfig])
    _ <- ZIO.log(s"Mediator APP. See https://github.com/input-output-hk/atala-prism-mediator")
    _ <- ZIO.log(s"MediatorConfig: $mediatorConfig")
    _ <- ZIO.log(s"DID: ${mediatorConfig.did.id.string}")
    myHub <- Hub.sliding[String](5)
    _ <- ZStream.fromHub(myHub).run(ZSink.foreach((str: String) => ZIO.logInfo("HUB: " + str))).fork
    port <- configs
      .nested("http")
      .nested("server")
      .nested("mediator")
      .load(Config.int("port"))
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
      .provideSomeLayer(mediatorConfig.agentLayer) // .provideSomeLayer(AgentByHost.layer)
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
