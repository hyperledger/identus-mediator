package io.iohk.atala.mediator.app

import fmgp.crypto.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.*
import fmgp.did.method.peer.*
import io.iohk.atala.mediator.actions.*
import io.iohk.atala.mediator.comm.*
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.protocols.*
import io.iohk.atala.mediator.utils.*
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.http.*
import zio.http.Http.{Empty, Static}
import zio.http.ZClient.ClientLive
import zio.json.*
import zio.logging.LogFormat.*
import zio.logging.backend.SLF4J
import zio.logging.*
import zio.stream.*

import java.time.format.DateTimeFormatter
import scala.io.Source
case class MediatorConfig(endpoint: java.net.URI, keyAgreement: OKPPrivateKey, keyAuthentication: OKPPrivateKey) {
  val did = DIDPeer2.makeAgent(
    Seq(keyAgreement, keyAuthentication),
    Seq(DIDPeerServiceEncoded(s = endpoint.toString()))
  )
  val agentLayer = ZLayer(MediatorAgent.make(id = did.id, keyStore = did.keyStore))
}
case class DataBaseConfig(
    protocol: String,
    host: String,
    port: Option[String],
    userName: String,
    password: String,
    dbName: String
) {
  private def maybePort = port.filter(_.nonEmpty).map(":" + _).getOrElse("")
  val connectionString = s"$protocol://$userName:$password@$host$maybePort/$dbName"
  val displayConnectionString = s"$protocol://$userName:******@$host$maybePort/$dbName"
  override def toString: String = s"""DataBaseConfig($protocol, $host, $port, $userName, "******", $dbName)"""
}

object MediatorStandalone extends ZIOAppDefault {
  val mediatorColorFormat: LogFormat =
    fiberId.color(LogColor.YELLOW) |-|
      line.highlight |-|
      allAnnotations |-|
      cause.highlight

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j(mediatorColorFormat)

  // val app: HttpApp[ // type HttpApp[-R, +Err] = Http[R, Err, Request, Response]
  //   Hub[String] & Operations & MessageDispatcher & MediatorAgent & Resolver & MessageItemRepo & UserAccountRepo &
  //     OutboxMessageRepo,
  //   Throwable
  // ]
  val app: Http[
    Operations & Resolver & UserAccountRepo & OutboxMessageRepo & MessageDispatcher & MediatorAgent & MessageItemRepo,
    (HttpAppMiddleware[Nothing, Any, Nothing, Any] | HttpAppMiddleware[Nothing, Any, Nothing, Any])#OutErr[Throwable],
    Request,
    Response
  ] = MediatorAgent.didCommApp
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
    mediatorDbConfig <- configs.nested("database").nested("mediator").load(deriveConfig[DataBaseConfig])
    _ <- ZIO.log(s"MediatorDb Connection String: ${mediatorDbConfig.displayConnectionString}")
    myHub <- Hub.sliding[String](5)
    _ <- ZStream.fromHub(myHub).run(ZSink.foreach((str: String) => ZIO.logInfo("HUB: " + str))).fork
    port <- configs
      .nested("http")
      .nested("server")
      .nested("mediator")
      .load(Config.int("port"))
    _ <- ZIO.log(s"Starting server on port: $port")
    escalateTo <- configs
      .nested("report")
      .nested("problem")
      .nested("mediator")
      .load(Config.string("escalateTo"))
    _ <- ZIO.log(s"Problem reports escalated to : $escalateTo")
    client = Scope.default >>> Client.default
    inboundHub <- Hub.bounded[String](5)
    myServer <- Server
      .serve(
        (app @@ (MiddlewareUtils.annotateHeaders ++ MiddlewareUtils.serverTime))
          .tapUnhandledZIO(ZIO.logError("Unhandled Endpoint"))
          .tapErrorCauseZIO(cause => ZIO.logErrorCause(cause)) // THIS is to log all the erros
          .mapError(err =>
            Response(
              status = Status.BadRequest,
              headers = Headers.empty,
              body = Body.fromString(err.toString()), // Body.fromString(err.getMessage()),
            )
          )
      )
      .provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
      .provideSomeLayer(mediatorConfig.agentLayer) // .provideSomeLayer(AgentByHost.layer)
      .provideSomeLayer(
        AsyncDriverResource.layer
          >>> ReactiveMongoApi.layer(mediatorDbConfig.connectionString)
          >>> MessageItemRepo.layer.and(UserAccountRepo.layer).and(OutboxMessageRepo.layer)
      )
      .provideSomeLayer(Operations.layerDefault)
      .provideSomeLayer(client >>> MessageDispatcherJVM.layer)
      .provideSomeEnvironment { (env: ZEnvironment[Server]) => env.add(myHub) }
      .provide(Server.defaultWithPort(port))
      .debug
      .fork
    _ <- ZIO.log(s"Mediator Started")
    _ <- myServer.join *> ZIO.log(s"Mediator End")
    _ <- ZIO.log(s"*" * 100)
  } yield ()

}
