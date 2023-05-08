package fmgp.did.comm.protocol.pickup3

import zio.json._
import fmgp.did._
import fmgp.did.comm._

/** Status details about waiting messages.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/messagepickup/3.0/status",
  *   "body": {
  *     "recipient_did": "<did for messages>",
  *     "message_count": 7,
  *     "longest_waited_seconds": 3600,
  *     "newest_received_time": 1658085169,
  *     "oldest_received_time": 1658084293,
  *     "total_bytes": 8096,
  *     "live_delivery": false
  *   }
  * }
  * }}}
  */
final case class Status(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    recipient_did: Option[FROMTO], // IMPROVE We can make this type-safe by splitting into two classes
    message_count: Int,
    longest_waited_seconds: Option[Long],
    newest_received_time: Option[Long], // TODO UTC Epoch Seconds
    oldest_received_time: Option[Long], // TODO UTC Epoch Seconds
    total_bytes: Option[Long],
    live_delivery: Option[Boolean],
) {
  def piuri = Status.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        Status
          .Body(
            recipient_did = recipient_did,
            message_count = message_count,
            longest_waited_seconds = longest_waited_seconds,
            newest_received_time = newest_received_time,
            oldest_received_time = oldest_received_time,
            total_bytes = total_bytes,
            live_delivery = live_delivery,
          )
          .toJSON_RFC7159
      )
    )
}

object Status {
  def piuri = PIURI("https://didcomm.org/messagepickup/3.0/status")

  protected final case class Body(
      recipient_did: Option[FROMTO],
      message_count: Int,
      longest_waited_seconds: Option[Long],
      newest_received_time: Option[Long], // TODO UTC Epoch Seconds
      oldest_received_time: Option[Long], // TODO UTC Epoch Seconds
      total_bytes: Option[Long],
      live_delivery: Option[Boolean]
  ) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Status] =
    if (msg.`type` != piuri) Left(s"No able to create Status from a Message of type '${msg.`type`}'")
    else
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
                        Right(
                          Status(
                            id = msg.id,
                            thid = thid,
                            from = from,
                            to = firstTo,
                            recipient_did = body.recipient_did,
                            message_count = body.message_count,
                            longest_waited_seconds = body.longest_waited_seconds,
                            newest_received_time = body.newest_received_time,
                            oldest_received_time = body.oldest_received_time,
                            total_bytes = body.total_bytes,
                            live_delivery = body.live_delivery,
                          )
                        )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
