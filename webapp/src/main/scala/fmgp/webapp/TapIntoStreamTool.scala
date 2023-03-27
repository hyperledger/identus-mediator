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
import fmgp.Websocket
import fmgp.WebsocketJSLive

object TapIntoStreamTool {

  val ws = WebsocketJSLive.autoReconnect

  val rootElement = div(
    onMountCallback { ctx =>
      // job(ctx.owner)
      ()
    },
    code("TapIntoStream Tool"),
    p("Tap into Alice's stream"),
    br(),
    div( // container
      padding := "12pt",
      // outgoingMgs(
      //   MsgID("TODO MSG TYPE"),
      //   Some(FROM("did:peer:123456789qwertyuiopasdfghjklzxcvbnm")),
      //   "TODO WIP",
      // ),
      children <-- ws.wsJS.income.signal.map(_.map(_.decrypted).map { msg =>
        BasicMessage.fromPlaintextMessage(msg) match
          case Left(ex)  => incomeMgs(msg.id, msg.from, msg.`type`.value)
          case Right(bm) => incomeMgs(msg.id, msg.from, bm.content)
      })
    ),
  )

  def apply(): HtmlElement = rootElement

  def incomeMgs(msgId: MsgID, did: Option[FROM], content: String) = div(
    display.flex,
    div( // message row Left (income)
      className := "mdc-card",
      maxWidth := "90%",
      margin := "4pt",
      padding := "8pt",
      display.flex,
      flexDirection.row,
      div(padding := "8pt", paddingBottom := "6pt", i(className("material-icons"), "face")), // avatar
      div(
        div(msgId.value),
        div(did.map(_.value).getOrElse("")),
        div(content),
      )
    )
  )
  def outgoingMgs(msgId: MsgID, did: Option[FROM], content: String) = div(
    display.flex,
    flexDirection.rowReverse,
    div( // message row Right (outgoing)
      className := "mdc-card",
      maxWidth := "90%",
      margin := "4pt",
      padding := "8pt",
      display.flex,
      flexDirection.rowReverse,
      div(padding := "8pt", paddingBottom := "6pt", i(className("material-icons"), "face")), // avatar
      div(
        div(msgId.value),
        div(did.map(_.value).getOrElse("")),
        div(content),
        // div(
        //   className := "mdc-card__primary-action",
        //   tabIndex := 0,
        //   "action",
        //   div(className := "mdc-card__ripple")
        // ),
      )
    )
  )
}
