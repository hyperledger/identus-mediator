package fmgp.did.comm.protocol.routing2

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

extension (msg: PlaintextMessage)
  def toForwardMessage: Either[String, ForwardMessage] =
    ForwardMessage.fromPlaintextMessage(msg)

extension (msg: EncryptedMessage)
  def toAttachmentJson: Either[String, Attachment] =
    msg.toJsonAST.map(json => Attachment(data = AttachmentDataJson(json = json)))

/** The Forward Message is sent by the sender to the mediator to forward (the data) to a recipient.
  *
  * {{{
  * {
  *   "type": "https://didcomm.org/routing/2.0/forward",
  *   "id": "abc123xyz456",
  *   "to": ["did:example:mediator"],
  *   "expires_time": 1516385931,
  *   "body":{ "next": "did:foo:1234abcd"},
  *   "attachments": [] //The payload(s) to be forwarded
  * }
  * }}}
  *
  * @param lang
  *   See [https://identity.foundation/didcomm-messaging/spec/#routing-protocol-20]
  */
final case class ForwardMessage(
    id: MsgID = MsgID(),
    to: Set[TO] = Set.empty,
    next: DIDSubject, // TODO is this on the type TO?
    expires_time: NotRequired[UTCEpoch] = None,
    attachments: Seq[Attachment],
) {
  def `type` = ForwardMessage.piuri

  def toPlaintextMessage(from: Option[FROM]): Either[String, PlaintextMessage] =
    ForwardMessage
      .Body(next)
      .toJSON_RFC7159
      .map(body =>
        PlaintextMessageClass(
          id = id,
          `type` = `type`,
          to = Some(to),
          body = body,
          expires_time = expires_time,
          attachments = Some(attachments),
        )
      )
}

object ForwardMessage {
  def piuri = PIURI("https://didcomm.org/routing/2.0/forward")

  protected final case class Body(next: DIDSubject) {
    def toJSON_RFC7159: Either[String, JSON_RFC7159] =
      this.toJsonAST.flatMap(_.as[JSON_RFC7159])
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, ForwardMessage] = {
    if (msg.`type` != piuri) Left(s"No able to create BasicMessage from a Message of the type '${msg.`type`}'")
    else
      msg.body.as[Body].map { body =>
        ForwardMessage(
          id = msg.id,
          to = msg.to.getOrElse(Set.empty),
          next = body.next,
          expires_time = msg.expires_time,
          attachments = msg.attachments.getOrElse(Seq.empty), // TODO error?
        )
      }
  }

  def buildForwardMessage(
      id: MsgID = MsgID(),
      to: Set[TO] = Set.empty,
      next: DIDSubject,
      msg: EncryptedMessage,
  ) =
    if (!msg.recipientsSubject.contains(next))
      Left("'next' shound be one of the recipients")
    else
      msg.toAttachmentJson.map(attachment =>
        ForwardMessage(
          id = id,
          to = to,
          next = next,
          attachments = Seq(attachment),
        )
      )

  // TODO make a test (but need a implementation )
  def makeForwardMessage(
      to: TO, // Mediator
      next: DIDSubject,
      msg: EncryptedMessage
  ): ZIO[Operations & Resolver, DidFail, EncryptedMessage] =
    buildForwardMessage(next = next, msg = msg, to = Set(to)) match
      case Left(error1) => ZIO.fail(FailToEncodeMessage(piuri, error1))
      case Right(forwardMessage) =>
        forwardMessage.toPlaintextMessage(from = None) match
          case Left(error2) => ZIO.fail(FailToEncodeMessage(piuri, error2))
          case Right(fMsg) =>
            for {
              ops <- ZIO.service[Operations]
              encryptedMessage <- ops.anonEncrypt(fMsg)
            } yield encryptedMessage

}
