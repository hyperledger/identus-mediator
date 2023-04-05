package fmgp.did.comm.protocol.pickup3
/* https://didcomm.org/mediator-coordination/2.0/ */

import zio.json._
import fmgp.did._
import fmgp.did.comm._

/** Live Mode is changed with a live-delivery-change message.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/messagepickup/3.0/live-delivery-change",
  *   "body": { "live_delivery": true }
  * }
  * }}}
  */
final case class LiveModeChange(id: MsgID = MsgID(), from: FROM, to: TO, live_delivery: Boolean) {
  def piuri = LiveModeChange.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(LiveModeChange.Body(live_delivery = live_delivery).toJSON_RFC7159)
    )
}
object LiveModeChange {
  def piuri = PIURI("https://didcomm.org/messagepickup/3.0/live-delivery-change")

  protected final case class Body(live_delivery: Boolean) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, LiveModeChange] =
    if (msg.`type` != piuri) Left(s"No able to create LiveModeChange from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from' with one element")
            case Some(from) =>
              msg.body match
                case None => Left(s"'$piuri' MUST have field 'body'")
                case Some(b) =>
                  b.as[Body].flatMap { body =>
                    Right(
                      LiveModeChange(
                        id = msg.id,
                        from = from,
                        to = firstTo,
                        live_delivery = body.live_delivery
                      )
                    )
                  }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
