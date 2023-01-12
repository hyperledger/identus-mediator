package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import com.raquo.laminar.api.L._

import fmgp.did.Agent
import fmgp.did.AgentProvider
import fmgp.did.resolver.peer.DIDPeer
import fmgp.did.DID

object Global {

  /** Agent in use */
  val agentVar = Var[Option[DIDPeer.AgentDIDPeer]](initial = None)

  val dids = AgentProvider.allAgents.keys.toSeq.sorted :+ "<none>"
  val didsTO = AgentProvider.allIdentities.keys.toSeq.sorted :+ "<none>"

  def getAgentName(mAgent: Option[Agent]): String =
    mAgent.flatMap(agent => AgentProvider.allAgents.find(_._2.id == agent.id)).map(_._1).getOrElse("<none>")

  def getIdentitiesName(mDID: Option[DID]): String =
    mDID.flatMap(did => AgentProvider.allIdentities.find(_._2.did == did.did)).map(_._1).getOrElse("<none>")

  def makeSelectElementDID(didVar: Var[Option[DID]]) = select(
    value <-- didVar.signal.map(getIdentitiesName(_)),
    onChange.mapToValue.map(e => AgentProvider.allIdentities.get(e)) --> didVar,
    Global.didsTO.map { step => option(value := step, step) }
  )

  def clipboardSideEffect(text: => String): Any => Unit =
    (_: Any) => { dom.window.navigator.clipboard.writeText(text) }

}
