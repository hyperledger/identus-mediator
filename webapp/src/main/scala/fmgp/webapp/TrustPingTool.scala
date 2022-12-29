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
import fmgp.did.comm.protocol.trustping2._
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver

object TrustPingTool {

  val fromAgentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val toAgentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val encryptedMessageVar: Var[Option[EncryptedMessage]] = Var(initial = None)

  val responseRequestedVar = Var(initial = true)

  // def message = inicialTextVar.signal.map(e => BasicMessage(content = e))

  def mTrustPing =
    Signal
      .combine(
        fromAgentVar,
        toAgentVar,
        responseRequestedVar
      )
      .map {
        case (mFrom, None, responseRequested) => Left("Missing the 'TO'")
        case (None, Some(to), true)           => Left("Missing the 'FROM' (since response_requested is true)")
        case (Some(from), Some(to), true) =>
          Right(TrustPingWithRequestedResponse(from = from.id, to = to.id))
        case (mFrom, Some(to), false) =>
          Right(TrustPingWithOutRequestedResponse(from = mFrom.map(_.id), to = to.id))
      }

  val job = Signal
    .combine(
      fromAgentVar,
      mTrustPing
    )
    .map {
      case (_, Left(error))                                  => encryptedMessageVar.update(_ => None)
      case (None, Right(tp: TrustPingWithRequestedResponse)) => ??? // Imposibel
      case (None, Right(tp: TrustPingWithOutRequestedResponse)) =>
        tp.toPlaintextMessage match
          case Left(value) => ??? // Imposibel
          case Right(message) =>
            val program =
              OperationsClientRPC
                .anonEncrypt(message)
                .map(msg => encryptedMessageVar.update(_ => Some(msg)))
            Unsafe.unsafe { implicit unsafe => // Run side efect
              Runtime.default.unsafe.fork(program.provideEnvironment(ZEnvironment(DidPeerResolver)))
            }
      case (Some(agent), Right(tp: TrustPing)) =>
        tp.toPlaintextMessage match
          case Left(value) => ??? // Imposibel
          case Right(message) =>
            val program =
              OperationsClientRPC
                .authEncrypt(message)
                .map(msg => encryptedMessageVar.update(_ => Some(msg)))
            Unsafe.unsafe { implicit unsafe => // Run side efect
              Runtime.default.unsafe.fork(program.provideEnvironment(ZEnvironment(agent, DidPeerResolver)))
            }
    }
    .observe(App.owner)

  val rootElement = div(
    code("TrustPing Tool"),
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
    p(
      "TO: ",
      select(
        value <-- toAgentVar.signal.map(Global.getAgentName(_)),
        onChange.mapToValue.map(e => AgentProvider.allAgents.get(e)) --> toAgentVar,
        Global.dids.map { step => option(value := step, step) }
      )
    ),
    pre(code(child.text <-- toAgentVar.signal.map(_.map(_.id.string).getOrElse("none")))),
    p("Requested Response:"),
    input(
      typ("checkbox"),
      checked <-- responseRequestedVar,
      onInput.mapToChecked --> responseRequestedVar
    ),
    p("Basic Message"),
    pre(code(child.text <-- mTrustPing.map(_.flatMap(_.toPlaintextMessage).map(_.toJsonPretty).merge))),
    p(
      "Encrypted Message",
      "(NOTE: This is executed as a RPC call to the JVM server, since the JS version has not yet been fully implemented)"
    ),
    pre(code(child.text <-- encryptedMessageVar.signal.map(_.toJsonPretty))),
  )

  def apply(): HtmlElement = rootElement
}
