package fmgp.did.comm.protocol.routing2

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.util.Base64

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

sealed trait ForwardMessage {
  def id: MsgID
  def to: Set[TO]
  def from: Option[FROM]
  def next: DIDSubject
  def expires_time: NotRequired[UTCEpoch]
  def msg: EncryptedMessage

  // methods
  def `type` = ForwardMessage.piuri

  def toAttachments: Attachment

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      body = ForwardMessage.Body(next).toJSON_RFC7159,
      expires_time = expires_time,
      attachments = Some(Seq(toAttachments)),
    )
}

final case class ForwardMessageBase64(
    id: MsgID = MsgID(),
    to: Set[TO] = Set.empty,
    from: Option[FROM],
    next: DIDSubject, // TODO is this on the type TO?
    expires_time: NotRequired[UTCEpoch] = None,
    msg: EncryptedMessage,
) extends ForwardMessage {
  def toAttachments: Attachment = Attachment(
    data = AttachmentDataBase64(Base64.encode(msg.toJson))
  )

}

final case class ForwardMessageJson(
    id: MsgID = MsgID(),
    to: Set[TO] = Set.empty,
    from: Option[FROM],
    next: DIDSubject, // TODO is this on the type TO? //IMPROVE next MUST? be one o recipients
    expires_time: NotRequired[UTCEpoch] = None,
    msg: EncryptedMessage,
) extends ForwardMessage {
  def toAttachments: Attachment = Attachment(
    /** toJSON_RFC7159 MUST not fail! */
    data = AttachmentDataJson(msg.toJsonAST.getOrElse(JSON_RFC7159()))
  )
}

object ForwardMessage {
  def piuri = PIURI("https://didcomm.org/routing/2.0/forward")

  protected final case class Body(next: DIDSubject) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, ForwardMessage] = {
    if (msg.`type` != piuri) Left(s"No able to create ForwardMessage from a Message of the type '${msg.`type`}'")
    else {

      msg.body
        .as[Body]
        .left
        .map(error => s"'$piuri' fail to parse body due to: $error")
        .flatMap { body =>
          msg.attachments match
            case None =>
              Left(s"'$piuri' MUST have Attachments (with one attachment that include the message to foward)")
            case Some(Seq()) => Left(s"'$piuri' MUST have one Attachment (with the message to foward)")
            case Some(firstAttachment +: Seq()) =>
              firstAttachment.data match {
                case AttachmentDataJWS(jws, links) =>
                  Left(s"'$piuri' MUST of the Attachment type Base64 or Json (instead of JWT)")
                case AttachmentDataLinks(links, hash) =>
                  Left(s"'$piuri' MUST of the Attachment type Base64 or Json (instead of Link)")
                case AttachmentDataBase64(base64) =>
                  base64.decodeToString.fromJson[EncryptedMessage] match
                    case Left(error) =>
                      Left(s"'$piuri' fail to parse the attachment (base64) as an EncryptedMessage due to: $error")
                    case Right(nextMsg) =>
                      Right(
                        ForwardMessageBase64(
                          id = msg.id,
                          to = msg.to.getOrElse(Set.empty),
                          from = msg.from,
                          next = body.next,
                          expires_time = msg.expires_time,
                          msg = nextMsg,
                        )
                      )
                case AttachmentDataJson(json) =>
                  json.as[EncryptedMessage] match
                    case Left(error) =>
                      Left(s"'$piuri' fail to parse the attachment (json) as an EncryptedMessage due to: $error")
                    case Right(nextMsg) =>
                      Right(
                        ForwardMessageJson(
                          id = msg.id,
                          to = msg.to.getOrElse(Set.empty),
                          from = msg.from,
                          next = body.next,
                          expires_time = msg.expires_time,
                          msg = nextMsg,
                        )
                      )
                case AttachmentDataAny(jws, hash, links, base64, json) =>
                  Left(s"'$piuri' has attachments of unknown type") // TODO shound we still try?
              }
            case Some(firstAttachments +: tail) =>
              Left(s"'$piuri' MUST have only one attachment (instead of multi attachment)")
            case Some(value) => // IMPOSIBLE
              Left(
                s"ERROR: '$piuri' fail to parse Attachment - This case SHOULD be IMPOSIBLE. value='$value"
              )
        }

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
      Right(
        ForwardMessageJson(
          id = id,
          to = to,
          from = None,
          next = next,
          msg = msg,
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
        for {
          ops <- ZIO.service[Operations]
          encryptedMessage <- ops.anonEncrypt(forwardMessage.toPlaintextMessage)
        } yield encryptedMessage

}
