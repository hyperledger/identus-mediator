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

object DIDCommHome {

  val mediatorDID = Agent0Mediators.agent.id.document
  val aliceDID = Agent1Mediators.agent.id.document
  val bobDID = Agent2Mediators.agent.id.document

  val aliceDIDVar: Var[Option[DIDDocument]] = Var(initial = Some(aliceDID))
  val bobDIDVar: Var[Option[DIDDocument]] = Var(initial = Some(bobDID))
  val inicialTextVar = Var(initial = "<Some text>")
  def message = inicialTextVar.signal.map(e => s"--$e--")
  def readMessage = message.map(e => s"### ${e} ###")

  val rootElement = div(
    code("DIDcomm Page"),
    p("From Alice:"),
    pre(code(child.text <-- aliceDIDVar.signal.map(_.map(e => e.toJsonPretty).getOrElse("--")))),
    p("To Bob:"),
    pre(code(child.text <-- bobDIDVar.signal.map(_.map(e => e.toJsonPretty).getOrElse("--")))),
    p("Message text:"),
    input(
      placeholder("Words"),
      `type`.:=("textbox"),
      autoFocus(true),
      value <-- inicialTextVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> inicialTextVar }
    ),
    p("Comm type: TODO"),
    p("Message after: TODO"),
    pre(code(child.text <-- message)),
    p("Bob read message: TODO"),
    pre(code(child.text <-- readMessage)),
  )
  def apply(): HtmlElement = rootElement
}
