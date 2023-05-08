package fmgp.did.comm.protocol.pickup3

import zio.json._
import fmgp.did._
import fmgp.did.comm._
import fmgp.util.Base64

/** Batch of messages delivered to the recipient as attachments.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "thid": "<message id of delivery-request message>",
  *   "type": "https://didcomm.org/messagepickup/3.0/delivery",
  *   "body": { "recipient_did": "<did for messages>" },
  *   "attachments": [ { "id": "<id of message>", "data": { "base64": "<message>" } } ]
  * }
  * }}}
  */
final case class MessageDelivery(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    recipient_did: Option[FROMTO], // IMPROVE We can make this type-safe by splitting into two classes
    attachments: Map[String, Message]
) {
  def piuri = MessageDelivery.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
      body = Some(MessageDelivery.Body(recipient_did = recipient_did).toJSON_RFC7159),
      attachments = Some(
        attachments.toSeq.map(e => Attachment(id = Some(e._1), data = AttachmentDataBase64(Base64.encode(e._2.toJson))))
      )
    )
}
object MessageDelivery {
  def piuri = PIURI("https://didcomm.org/messagepickup/3.0/delivery")

  protected final case class Body(recipient_did: Option[FROMTO]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MessageDelivery] =
    if (msg.`type` != piuri) Left(s"No able to create MessageDelivery from a Message of type '${msg.`type`}'")
    else {
      def auxAttachments = msg.attachments.toSeq.flatten
        .map { attachment =>
          attachment.id match
            case None => Left(s"'$piuri' MUST have id in all Attachments")
            case Some(id) =>
              attachment.data match
                case AttachmentDataJWS(jws, links)    => Left(s"'$piuri' MUST have all Attachments in Base64")
                case AttachmentDataLinks(links, hash) => Left(s"'$piuri' MUST have all Attachments in Base64")
                case AttachmentDataJson(json)         => Left(s"'$piuri' MUST have all Attachments in Base64")
                case AttachmentDataAny(jws, hash, links, None, json) =>
                  Left(s"'$piuri' MUST have all Attachments in Base64")
                case AttachmentDataAny(jws, hash, links, Some(base64), json) => Right(id -> base64)
                case AttachmentDataBase64(base64)                            => Right(id -> base64)
        }
        .map(_.flatMap(kv => kv._2.decodeToString.fromJson[Message].map(m => (kv._1, m))))
        .foldRight(Right(Seq.empty): Either[String, Seq[(String, Message)]]) { case (elem, acc) =>
          for { xs <- acc; x <- elem } yield x +: xs
        }

      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.thid match
                  case None => Left(s"'$piuri' MUST have field 'thid'")
                  case Some(thid) =>
                    msg.from match
                      case None => Left(s"'$piuri' MUST have field 'from' with one element")
                      case Some(from) =>
                        auxAttachments.map(attachments =>
                          MessageDelivery(
                            id = msg.id,
                            thid = thid,
                            from = from,
                            to = firstTo,
                            recipient_did = body.recipient_did,
                            attachments = attachments.toMap
                          )
                        )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
    }
}
