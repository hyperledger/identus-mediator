package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import com.raquo.laminar.api.L._

import fmgp.did.Agent
import fmgp.did.AgentProvider

object Global {

  val dids = AgentProvider.allAgents.keys.toSeq.sorted :+ "<none>"
  def getAgentName(mAgent: Option[Agent]): String =
    mAgent.flatMap(agent => AgentProvider.allAgents.find(_._2.id == agent.id)).map(_._1).getOrElse("<none>")

  def clipboardSideEffect(text: => String): Any => Unit =
    (_: Any) => { dom.window.navigator.clipboard.writeText(text) }

}
