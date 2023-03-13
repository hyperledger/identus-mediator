package fmgp.did.comm.protocol.pickup3

import zio.json._
import fmgp.did._
import fmgp.did.comm._

/** After receiving messages, the recipient sends an acknowledge message indiciating which messages are safe to clear
  * from the queue.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/messagepickup/3.0/messages-received",
  *   "body": { "message_id_list": ["123","456"] }
  * }
  * }}}
  */
final case class MessagesReceived(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    message_id_list: Seq[String], // Seq[MsgID],
) {
  def piuri = MessagesReceived.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
      body = MessagesReceived.Body(message_id_list = message_id_list).toJSON_RFC7159
    )
}
object MessagesReceived {
  def piuri = PIURI("https://didcomm.org/messagepickup/3.0/messages-received")

  protected final case class Body(message_id_list: Seq[String]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MessagesReceived] =
    if (msg.`type` != piuri) Left(s"No able to create MessagesReceived from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body
            .as[Body]
            .flatMap(body =>
              msg.thid match
                case None => Left(s"'$piuri' MUST have field 'thid'")
                case Some(thid) =>
                  msg.from match
                    case None => Left(s"'$piuri' MUST have field 'from' with one element")
                    case Some(from) =>
                      Right(
                        MessagesReceived(
                          id = msg.id,
                          thid = thid,
                          from = from,
                          to = firstTo,
                          message_id_list = body.message_id_list,
                        )
                      )
            )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
