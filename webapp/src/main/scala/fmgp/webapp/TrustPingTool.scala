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
import fmgp.did.comm.protocol.trustping2._
import fmgp.did.method.peer.DIDPeer
import fmgp.did.method.peer.DidPeerResolver

import fmgp.did.AgentProvider
object TrustPingTool {

  val toDIDVar: Var[Option[DID]] = Var(initial = None)
  val responseRequestedVar = Var(initial = true)
  val mTrustPingVar: Var[Either[String, TrustPingWithRequestedResponse | TrustPingWithOutRequestedResponse]] =
    Var(initial = Left("Inicial State"))

  def job(owner: Owner) =
    Signal
      .combine(
        Global.agentVar,
        toDIDVar,
        responseRequestedVar
      )
      .map {
        case (mFrom, None, responseRequested) => Left("Missing the 'TO'")
        case (None, Some(to), true)           => Left("Missing the 'FROM' (since response_requested is true)")
        case (Some(from), Some(to), true) =>
          Right(TrustPingWithRequestedResponse(from = from.id, to = to))
        case (mFrom, Some(to), false) =>
          Right(TrustPingWithOutRequestedResponse(from = mFrom.map(_.id), to = to))
      }
      .map(e => mTrustPingVar.set(e))
      .observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      job(ctx.owner)
      ()
    },
    code("TrustPing Tool"),
    p(
      overflowWrap.:=("anywhere"),
      "FROM: ",
      " ",
      code(child.text <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("none")))
    ),
    p(
      overflowWrap.:=("anywhere"),
      "TO: ",
      Global.makeSelectElementDID(toDIDVar),
      " ",
      code(child.text <-- toDIDVar.signal.map(_.map(_.string).getOrElse("none"))),
    ),
    p("Requested Response:"),
    input(
      typ("checkbox"),
      checked <-- responseRequestedVar,
      onInput.mapToChecked --> responseRequestedVar
    ),
    p("TrustPint"),
    pre(code(child.text <-- mTrustPingVar.signal.map(_.map(_.toPlaintextMessage.toJsonPretty).merge))),
    div(
      child <-- {
        mTrustPingVar.signal
          .map(_.map(_.toPlaintextMessage.toJsonPretty))
          .map {
            case Left(error) => new CommentNode("")
            case Right(json) =>
              button(
                "Copy to Encrypt Tool",
                disabled <-- mTrustPingVar.signal.map(_.isLeft),
                onClick --> { _ => EncryptTool.dataTextVar.set(json) },
                MyRouter.navigateTo(MyRouter.EncryptPage)
              )
          }
      }
    ),
  )

  def apply(): HtmlElement = rootElement
}
