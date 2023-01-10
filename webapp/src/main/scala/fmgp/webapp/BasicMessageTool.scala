package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.did.resolver.peer.DIDPeer

object BasicMessageTool {

  val fromAgentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val toDIDVar: Var[Option[DID]] = Var(initial = None)
  val encryptedMessageVar: Var[Option[EncryptedMessage]] = Var(initial = None)

  val inicialTextVar = Var(initial = "Hello, World!")
  def message = inicialTextVar.signal.map(e => BasicMessage(content = e))
  def plaintextMessage =
    Signal
      .combine(
        fromAgentVar,
        toDIDVar,
        message
      )
      .map {
        case (mFrom, Some(to), msg) => msg.toPlaintextMessage(mFrom.map(_.id), Set(to))
        case (mFrom, None, msg)     => Left("Missing the 'TO'")
      }

  val job = Signal
    .combine(
      fromAgentVar,
      toDIDVar,
      message
    )
    .map {
      case (mFrom, None, msg) =>
        encryptedMessageVar.update(_ => None)
      case (None, Some(to), msg) =>
        val tmp = msg.toPlaintextMessage(None, Set(to)).toOption.get
        val programAux = OperationsClientRPC.anonEncrypt(tmp)
        val program = programAux.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(DidPeerResolver))
          )
        }
      case (Some(from), Some(to), msg) =>
        val tmp = msg.toPlaintextMessage(Some(from.id), Set(to)).toOption.get
        val programAux = OperationsClientRPC.authEncrypt(tmp)
        val program = programAux.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(from, DidPeerResolver))
          )
        }
    }
    .observe(App.owner)

  val rootElement = div(
    code("BasicMessage Page"),
    p(
      "FROM: ",
      select(
        value <-- fromAgentVar.signal.map(Global.getAgentName(_)),
        onChange.mapToValue.map(e => AgentProvider.allAgents.get(e)) --> fromAgentVar,
        Global.dids.map { step => option(value := step, step) }
      )
    ),
    pre(code(child.text <-- fromAgentVar.signal.map(_.map(_.id.string).getOrElse("none")))),
    // pre(code(child.text <-- fromAgentVar.signal.map(_.map(e => e.id.document.toJsonPretty).getOrElse("--")))),
    p("TO: ", Global.makeSelectElementDID(toDIDVar)),
    pre(code(child.text <-- toDIDVar.signal.map(_.map(_.string).getOrElse("none")))),
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
    button(
      "Copy to clipboard",
      onClick --> Global.clipboardSideEffect(
        encryptedMessageVar.now() match
          case None        => "None"
          case Some(value) => value.toJson
      )
    )
  )

  def apply(): HtmlElement = rootElement
}
