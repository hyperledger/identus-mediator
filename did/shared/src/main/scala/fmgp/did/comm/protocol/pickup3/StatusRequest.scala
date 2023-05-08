package fmgp.did.comm.protocol.pickup3

import zio.json._
import fmgp.did._
import fmgp.did.comm._

/** Sent by the recipient to the mediator to request a status message.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/messagepickup/3.0/status-request",
  *   "body" : { "recipient_did": "<did for messages>" },
  *   "return_route": "all"
  * }
  * }}}
  */
final case class StatusRequest(id: MsgID = MsgID(), from: FROM, to: TO, recipient_did: Option[FROMTO]) {
  def piuri = StatusRequest.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
    )
}
object StatusRequest {
  def piuri = PIURI("https://didcomm.org/messagepickup/3.0/status-request")

  protected final case class Body(recipient_did: Option[FROMTO]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, StatusRequest] =
    if (msg.`type` != piuri) Left(s"No able to create StatusRequest from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.from match
                  case None => Left(s"'$piuri' MUST have field 'from' with one element")
                  case Some(from) =>
                    Right(
                      StatusRequest(
                        id = msg.id,
                        from = from,
                        to = firstTo,
                        recipient_did = body.recipient_did
                      )
                    )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
