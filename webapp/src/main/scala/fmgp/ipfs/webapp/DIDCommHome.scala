package fmgp.ipfs.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.example._
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage

object DIDCommHome {

  val dids = AgentProvider.allAgents.keys.toSeq.sorted :+ "<none>"
  def getAgentName(mAgent: Option[Agent]): String =
    mAgent.flatMap(agent => AgentProvider.allAgents.find(_._2.id == agent.id)).map(_._1).getOrElse("<none>")

  val fromAgentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val toAgentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)

  val inicialTextVar = Var(initial = "Hello, World!")
  def message = inicialTextVar.signal.map(e => BasicMessage(content = e))
  def plaintextMessage =
    Signal
      .combine(
        fromAgentVar,
        toAgentVar,
        message
      )
      .map {
        case (mFrom, Some(to), msg) => msg.toPlaintextMessage(mFrom.map(_.id), Set(to.id))
        case (mFrom, None, msg)     => Left("Missing the 'TO'")
      }

  def readMessage = message.map(e => s"### ${e} ###")

  val rootElement = div(
    code("DIDcomm Page"),
    p(
      "FROM: ",
      select(
        value <-- fromAgentVar.signal.map(getAgentName(_)),
        onChange.mapToValue.map(e => AgentProvider.allAgents.get(e)) --> fromAgentVar,
        dids.map { step => option(value := step, step) } // "" to work around a bug?
      )
    ),
    pre(code(child.text <-- fromAgentVar.signal.map(_.map(_.id.string).getOrElse("none")))),
    // pre(code(child.text <-- fromAgentVar.signal.map(_.map(e => e.id.document.toJsonPretty).getOrElse("--")))),
    p(
      "TO: ",
      select(
        value <-- toAgentVar.signal.map(getAgentName(_)),
        onChange.mapToValue.map(e => AgentProvider.allAgents.get(e)) --> toAgentVar,
        dids.map { step => option(value := step, step) } // "" to work around a bug?
      )
    ),
    pre(code(child.text <-- toAgentVar.signal.map(_.map(_.id.string).getOrElse("none")))),
    // pre(code(child.text <-- fromAgentVar.signal.map(_.map(e => e.id.document.toJsonPretty).getOrElse("--")))),
    p("BasicMessage text:"),
    input(
      placeholder("Words"),
      `type`.:=("textbox"),
      autoFocus(true),
      value <-- inicialTextVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> inicialTextVar }
    ),
    p("Basic Message"),
    pre(code(child.text <-- message.map(_.toString))),
    p("Plaintext Message"),
    pre(code(child.text <-- plaintextMessage.map(_.map(_.toJsonPretty).merge))),
    p("Comm type: TODO"),
    p("Message after: TODO"),
    p("Bob read message: TODO"),
    pre(code(child.text <-- readMessage)),
  )
  def apply(): HtmlElement = rootElement
}
