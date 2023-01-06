package fmgp.webapp

import com.raquo.laminar.api.L._

import fmgp.did.Agent
import fmgp.did.example.AgentProvider

object Global {

  val dids = AgentProvider.allAgents.keys.toSeq.sorted :+ "<none>"
  def getAgentName(mAgent: Option[Agent]): String =
    mAgent.flatMap(agent => AgentProvider.allAgents.find(_._2.id == agent.id)).map(_._1).getOrElse("<none>")

}
