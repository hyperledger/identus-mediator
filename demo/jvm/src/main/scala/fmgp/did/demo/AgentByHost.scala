package fmgp.did.demo

import zio._
import zio.json._
import zio.stream._
import zio.http.Request

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.mediator.MediatorAgent
import zio.http.model.Headers
import zio.http.model.headers.HeaderNames

object MyHeaders { // extends HeaderNames {
  final val xForwardedHost: CharSequence = "x-forwarded-host"
}

object AgentByHost {

  def getAgentFor(req: Request) = ZIO
    .serviceWithZIO[AgentByHost](_.agentFromRequest(req))
    .mapError(ex => DidException(ex))

  def getAgentFor(host: Host) = ZIO
    .serviceWithZIO[AgentByHost](_.agentFromHost(host))
    .mapError(ex => DidException(ex))

  def provideAgentFor[R, E <: Exception, A](req: Request, job: ZIO[R & MediatorAgent, E, A]) =
    for {
      agent <- ZIO
        .serviceWithZIO[AgentByHost](_.agentFromRequest(req))
        .mapError(ex => DidException(ex))
      ret <- job.provideSomeEnvironment((env: ZEnvironment[R]) => env.add(agent))
    } yield ()

  def hostFromRequest(req: Request): Option[Host] =
    req.headers
      .get(MyHeaders.xForwardedHost)
      .orElse(req.headers.host)
      .map(_.toString) // CharSequence -> String
      .map { // A bit of a hack to support a not standards http client
        case str if str.endsWith(":443") => str.dropRight(4)
        case str if str.endsWith(":80")  => str.dropRight(3)
        case str                         => str
      }
      .map(Host(_))

  val layer = ZLayer(
    for {
      // Host.fabio -> AgentProvider.fabio TODO
      alice <- MediatorAgent.make(AgentProvider.alice)
      bob <- MediatorAgent.make(AgentProvider.bob)
      charlie <- MediatorAgent.make(AgentProvider.charlie)
      local <- MediatorAgent.make(AgentProvider.local)
    } yield AgentByHost(
      Map(
        Host.alice -> alice,
        Host.bob -> bob,
        Host.charlie -> charlie,
        Host("localhost:8080") -> local,
      )
    )
  )
}

case class AgentByHost(agents: Map[Host, MediatorAgent]) {

  def agentFromRequest(req: Request): zio.ZIO[Any, NoAgent, MediatorAgent] =
    AgentByHost.hostFromRequest(req) match
      case None => ZIO.fail(NoAgent(s"Unknown host"))
      case Some(host) =>
        agents.get(host) match
          case None        => ZIO.fail(NoAgent(s"No Agent config for $host"))
          case Some(agent) => ZIO.succeed(agent)

  def agentFromHost(host: Host): zio.ZIO[Any, NoAgent, MediatorAgent] =
    agents.get(host) match
      case None        => ZIO.fail(NoAgent(s"No Agent config for $host"))
      case Some(agent) => ZIO.succeed(agent)
}
