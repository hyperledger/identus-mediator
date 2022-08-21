package fmgp.ipfs.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._

object DIDCommHome {

  val aliceDID = DIDDocumentClass(
    id = DIDSubject("did:dns:alice"),
    keyAgreement = Some(
      Set(
        VerificationMethodClass(
          id = "did:dns:alice#aaaaaaaaaaaaaaaaaaaaaaaaa",
          controller = "did:dns:alice",
          `type` = "X25519KeyAgreementKey2019",
          publicKeyMultibase = Some("z9hFgmPVfmBZwRvFEyniQDBkz9LmV7gDEqytWyGZLmDXE"),
        )
      )
    )
  )
  val bobDID = DIDDocumentClass(
    id = DIDSubject("did:dns:bob"),
    keyAgreement = Some(
      Set(
        VerificationMethodClass(
          id = "did:dns:bob#bbbbbbbbbbbbbbbbbbbbbbbbb",
          controller = "did:dns:bob",
          `type` = "X25519KeyAgreementKey2019",
          publicKeyMultibase = Some("z9hFgmPVfmBZwRvFEyniQDBkz9LmV7gDEqytWyGZLmDXE"),
        )
      )
    )
  )

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
