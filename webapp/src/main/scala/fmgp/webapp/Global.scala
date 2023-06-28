package fmgp.webapp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L._

import fmgp.did._
import fmgp.did.method.peer.DIDPeer
import fmgp.did.comm.TO

import fmgp.did.comm._

object Global {

  def mediatorDID = FROM(
    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9rOHMtaW50LmF0YWxhcHJpc20uaW8vbWVkaWF0b3IiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19"
  )
  def clipboardSideEffect(text: => String): Any => Unit =
    (_: Any) => { dom.window.navigator.clipboard.writeText(text) }

}
