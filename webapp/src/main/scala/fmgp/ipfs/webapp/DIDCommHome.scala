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
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import com.raquo.airstream.ownership._
object DIDCommHome {

  val owner: ManualOwner = ManualOwner()

  val dids = AgentProvider.allAgents.keys.toSeq.sorted :+ "<none>"
  def getAgentName(mAgent: Option[Agent]): String =
    mAgent.flatMap(agent => AgentProvider.allAgents.find(_._2.id == agent.id)).map(_._1).getOrElse("<none>")

  val fromAgentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val toAgentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val encryptedMessageVar: Var[Option[EncryptedMessage]] = Var(initial = None)

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

  val encryptedMessage = Signal
    .combine(
      fromAgentVar,
      toAgentVar,
      message
    )
    .map {
      case (mFrom, None, msg) =>
        encryptedMessageVar.update(_ => None)
      case (None, Some(to), msg) =>
        val tmp = msg.toPlaintextMessage(None, Set(to.id)).toOption.get
        val programAux = OperationsClientRPC.anonEncrypt(tmp)
        val program = programAux.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(DidPeerResolver))
          )
        }
      case (Some(from), Some(to), msg) =>
        val tmp = msg.toPlaintextMessage(Some(from.id), Set(to.id)).toOption.get
        val programAux = OperationsClientRPC.authEncrypt(tmp)
        val program = programAux.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(from, DidPeerResolver))
          )
        }
    }
    .toObservable
    .observe(owner)

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
    p(
      "Encrypted Message",
      "(NOTE: This is executed as a RPC call to the JVM server, since the JS version has not yet been fully implemented)"
    ),
    pre(code(child.text <-- encryptedMessageVar.signal.map(_.toJsonPretty))),
  )

  def apply(): HtmlElement = rootElement
}
