package io.iohk.atala.mediator

import org.scalajs.dom
import com.raquo.laminar.api.L._
import typings.qrcodeGenerator

import zio.json._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.oobinvitation.OOBInvitation

object MediatorInfo {

  val invitation = OOBInvitation(
    from = Global.mediatorDID,
    goal_code = Some("request-mediate"),
    goal = Some("RequestMediate"),
    accept = Some(Seq("didcomm/v2")),
  )
  val qrCodeData = OutOfBandPlaintext.from(invitation.toPlaintextMessage).makeURI("#/")

  val divQRCode = div()
  {
    val aux = qrcodeGenerator.mod.^.apply(qrcodeGenerator.TypeNumber.`0`, qrcodeGenerator.ErrorCorrectionLevel.L)
    aux.addData(qrCodeData)
    aux.make()
    divQRCode.ref.innerHTML = aux.createSvgTag(8d)
  }

  def apply(): HtmlElement = // rootElement
    div(
      h1("Invite for the DID Comm Mediator:"),
      h3("Plaintext out of band invitation:"),
      p(a(href := qrCodeData, target := "_blank", code(qrCodeData))), // FIXME make it a link to the mobile app
      pre(code(invitation.toPlaintextMessage.toJsonPretty)),
      pre(
        "To facilitate the integration with other systems you can get the plain text invitation and the out-of-band invitation on the following endpoints:",
        " '/invitation' and '/invitationOOB'"
      ),
      divQRCode,
      h3("Signed out of band invitation:"),
      code("TODO"),
    )

}
