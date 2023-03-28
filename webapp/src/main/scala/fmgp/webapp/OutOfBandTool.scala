package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.crypto.error._
import fmgp.webapp.MyRouter.OOBPage

object OutOfBandTool {

  def apply(oobPageSignal: Signal[OOBPage]): HtmlElement = div(
    onMountCallback { ctx =>
      // job(ctx.owner)
      ()
    },
    code("OutOfBand Tool Page"),
    p(
      overflowWrap.:=("anywhere"),
      div(child <-- oobPageSignal.map { e =>
        div(
          p(
            overflowWrap.:=("anywhere"),
            pre(code(s"OOB data: '${e.query_oob}'"))
          ),
          input(
            placeholder("OOB data"),
            autoFocus(true),
            value := e.query_oob,
            inContext { thisNode =>
              // Note: mapTo below accepts parameter by-name, evaluating it on every enter key press
              AppUtils.onEnterPress.mapTo(thisNode.ref.value) --> { data =>
                MyRouter.router.pushState(MyRouter.OOBPage(data))
              }
            }
          ),
          p("Message:"),
          OutOfBand.safeBase64(e.query_oob) match
            case Left(value)                          => value
            case Right(OutOfBandPlaintext(msg, data)) => pre(code(msg.toJsonPretty))
            case Right(OutOfBandSigned(msg, data))    => pre(code(msg.toJsonPretty))
        )
      })
    ),
  )

}
