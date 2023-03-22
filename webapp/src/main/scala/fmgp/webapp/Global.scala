package fmgp.webapp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L._

import fmgp.did._
import fmgp.did.resolver.peer.DIDPeer
import fmgp.did.comm.TO

object Global {

  /** Agent in use */
  val agentVar = Var[Option[Agent]](initial = None)
  val recipientVar = Var[Option[TO]](initial = None)

  val dids = AgentProvider.allAgents.keys.toSeq.sorted :+ "<none>"
  val didsTO = AgentProvider.allIdentities.keys.toSeq.sorted :+ "<none>"

  def agent2Host(mAgent: Option[Agent]): Option[String] = getAgentName(mAgent) match {
    case "alice"   => Some("alice.did.fmgp.app")
    case "bob"     => Some("bob.did.fmgp.app")
    case "charlie" => Some("charlie.did.fmgp.app")
    case _         => None
  }

  def getAgentName(mAgent: Option[Agent]): String =
    mAgent.flatMap(agent => AgentProvider.allAgents.find(_._2.id == agent.id)).map(_._1).getOrElse("<none>")

  def getIdentitiesName(mDID: Option[DID]): String =
    mDID.flatMap(did => AgentProvider.allIdentities.find(_._2.did == did.did)).map(_._1).getOrElse("<none>")

  def makeSelectElementDID(didVar: Var[Option[DID]]) = select(
    value <-- didVar.signal.map(getIdentitiesName(_)),
    onChange.mapToValue.map(e => AgentProvider.allIdentities.get(e)) --> didVar,
    Global.didsTO.map { step => option(value := step, step) }
  )
  def makeSelectElementTO(didVar: Var[Option[TO]]) = select(
    value <-- didVar.signal.map(_.map(_.toDID)).map(getIdentitiesName(_)),
    onChange.mapToValue.map(e => AgentProvider.allIdentities.get(e)).map(_.map(e => e: TO)) --> didVar,
    Global.didsTO.map { step => option(value := step, step) }
  )

  def clipboardSideEffect(text: => String): Any => Unit =
    (_: Any) => { dom.window.navigator.clipboard.writeText(text) }

  @JSExport
  def update(htmlPath: String) = {
    println("MermaidApp Update!!")
    val config = typings.mermaid.configTypeMod.MermaidConfig()
    typings.mermaid.mod.default.init(config, htmlPath)
  }
}
