package io.iohk.atala.mediator

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L._

import fmgp.did._
import fmgp.did.method.peer.DIDPeer
import fmgp.did.comm.TO

import fmgp.did.comm._

object Global {

  var mediatorDID = {
    val didSTR = dom.document.querySelector("""meta[name="did"]""")
    FROM(didSTR.getAttribute("content"))
  }

  def clipboardSideEffect(text: => String): Any => Unit =
    (_: Any) => { dom.window.navigator.clipboard.writeText(text) }

}
