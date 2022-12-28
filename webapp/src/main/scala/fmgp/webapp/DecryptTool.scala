package fmgp.webapp

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
import fmgp.crypto.error._

object DecryptTool {

  val agentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val dataVar: Var[String] = Var(initial = "")
  val encryptedMessageVar: Signal[Either[String, EncryptedMessage]] =
    // Var(initial = Left("<EncryptedMessage>"))
    dataVar.signal.map(_.fromJson[EncryptedMessage])
  val decryptMessageVar: Var[Option[Either[DidFail, Message]]] = Var(initial = None)

  val job =
    Signal
      .combine(
        agentVar,
        encryptedMessageVar
      )
      .map {
        case (None, _) =>
          decryptMessageVar.update(_ => None)
        case (Some(agent), Left(error)) =>
          decryptMessageVar.update(_ => Some(Left(FailToParse("Fail to parse Encrypted Message: " + error))))
        case (Some(agent), Right(msg)) =>
          val program = {
            msg.`protected`.obj match
              case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
                OperationsClientRPC.anonDecrypt(msg)
              case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
                OperationsClientRPC.authDecrypt(msg)
          }.mapBoth(
            error => decryptMessageVar.update(_ => Some(Left(error))),
            msg => decryptMessageVar.update(_ => Some(Right(msg)))
          )
          Unsafe.unsafe { implicit unsafe => // Run side efect
            Runtime.default.unsafe.fork(
              program.provideEnvironment(ZEnvironment(agent, DidPeerResolver))
            )
          }
      }
      .observe(App.owner)

  val rootElement = div(
    code("DecryptTool Page"),
    p(
      "Agent: ",
      select(
        value <-- agentVar.signal.map(Global.getAgentName(_)),
        onChange.mapToValue.map(e => AgentProvider.allAgents.get(e)) --> agentVar,
        Global.dids.map { step => option(value := step, step) }
      )
    ),
    pre(code(child.text <-- agentVar.signal.map(_.map(_.id.string).getOrElse("none")))),
    p("Encrypted Message Data:"),
    input(
      placeholder("<EncryptedMessage>"),
      `type`.:=("textbox"),
      autoFocus(true),
      value <-- dataVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> dataVar }
    ),
    p("Message after decrypt"),
    pre(code(child.text <-- decryptMessageVar.signal.map {
      case None                => "<none>"
      case Some(Left(didFail)) => didFail.toString
      case Some(Right(msg))    => msg.toJsonPretty
    })),
  )

  def apply(): HtmlElement = rootElement
}
