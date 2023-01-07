package fmgp.did.demo

import zio._
import zio.json._
import zio.stream._
import zio.http.Request

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.mediator.MediatorAgent

object AgentByHost {
  val HTTPHeader_host = "host"

  def getAgentFor(req: Request) =
    ZIO
      .serviceWithZIO[AgentByHost](_.agentFromRequest(req))
      .mapError(ex => DidException(ex))

  def provideAgentFor[R, E <: Exception, A](req: Request, job: ZIO[R & MediatorAgent, E, A]) =
    for {
      agent <- ZIO
        .serviceWithZIO[AgentByHost](_.agentFromRequest(req))
        .mapError(ex => DidException(ex))
      ret <- job.provideSomeEnvironment((env: ZEnvironment[R]) => env.add(agent))
    } yield ()

  def hostFromRequest(req: Request) = req
    .header(AgentByHost.HTTPHeader_host)
    .map { e => Host(e.value.toString) }

  val layer = ZLayer(
    for {
      // Host.fabio -> AgentProvider.fabio TODO
      alice <- MediatorAgent.make(AgentProvider.alice)
      bob <- MediatorAgent.make(AgentProvider.bob)
      charlie <- MediatorAgent.make(AgentProvider.charlie)
    } yield AgentByHost(
      Map(
        Host.alice -> alice,
        Host.bob -> bob,
        Host.charlie -> charlie,
      )
    )
  )

  // val layerWithHosts = ZLayer(
  //   for {
  //     // Host.fabio -> AgentProvider.fabio TODO
  //     alice <- MediatorAgent.make(AgentProvider.alice)
  //     bob <- MediatorAgent.make(AgentProvider.bob)
  //     charlie <- MediatorAgent.make(AgentProvider.charlie)
  //   } yield (
  //     Map(
  //       Host.alice -> alice,
  //       Host.bob -> bob,
  //       Host.charlie -> charlie,
  //     )
  //   )
  // )
}

case class AgentByHost(agents: Map[Host, MediatorAgent]) {

  def agentFromRequest(req: Request): zio.ZIO[Any, NoAgent, MediatorAgent] =
    AgentByHost.hostFromRequest(req) match
      case None => ZIO.fail(NoAgent(s"Unknown host"))
      case Some(host) =>
        agents.get(host) match
          case None        => ZIO.fail(NoAgent(s"No Agent config for $host"))
          case Some(agent) => ZIO.succeed(agent)
}
