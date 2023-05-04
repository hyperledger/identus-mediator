package fmgp.webapp

import org.scalajs.dom
import com.raquo.laminar.api.L._
import typings.qrcodeGenerator

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.oobinvitation.OOBInvitation

object MediatorInfo {

  val invitation = OOBInvitation(
    from = AgentProvider.alice.id,
    goal_code = Some("request-mediate"),
    goal = Some("RequestMediate"),
    accept = Some(Seq("didcomm/v2")),
  )
  val qrCodeData = OutOfBandPlaintext.from(invitation.toPlaintextMessage).makeURI("https://did.fmgp.app/#/")

  val divQRCode = div()
  {
    val aux = qrcodeGenerator.mod.^.apply(qrcodeGenerator.TypeNumber.`0`, qrcodeGenerator.ErrorCorrectionLevel.L)
    aux.addData(qrCodeData)
    aux.make()
    divQRCode.ref.innerHTML = aux.createSvgTag(8d)
  }

  def apply(): HtmlElement = // rootElement
    div(
      h1("DIDComm v2 Mediator (Alice) invite:"),
      h3("Plaintext out of band invitation:"),
      p(code(qrCodeData)),
      divQRCode,
      h3("Signed out of band invitation:"),
      code("TODO"),
    )

}
