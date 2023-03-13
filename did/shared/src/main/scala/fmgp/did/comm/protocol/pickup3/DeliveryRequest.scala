package fmgp.did.comm.protocol.pickup3

import zio.json._
import fmgp.did._
import fmgp.did.comm._

/** A request from the recipient to the mediator to have pending messages delivered.
  *
  * @param recipient_did
  *   Optional. When specified, the mediator MUST only return status related to that recipient did. This allows the
  *   recipient to discover if any messages are in the queue that were sent to a specific did.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "ttps://didcomm.org/messagepickup/3.0/delivery-request",
  *   "body": { "limit": 10, "recipient_did": "<did for messages>" },
  *   "return_route": "all"
  * }
  * }}}
  */
final case class DeliveryRequest(
    id: MsgID = MsgID(),
    from: FROM,
    to: TO,
    limit: Int,
    recipient_did: Option[FROMTO], // IMPROVE We can make this type-safe by splitting into two classes
) {
  def piuri = DeliveryRequest.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      to = Some(Set(to)),
      from = Some(from),
      body = DeliveryRequest.Body(limit = limit, recipient_did = recipient_did).toJSON_RFC7159
    )
}
object DeliveryRequest {
  def piuri = PIURI("https://didcomm.org/messagepickup/3.0/delivery-request")

  protected final case class Body(limit: Int, recipient_did: Option[FROMTO]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, DeliveryRequest] =
    if (msg.`type` != piuri) Left(s"No able to create DeliveryRequest from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body
            .as[Body]
            .flatMap(body =>
              msg.from match
                case None => Left(s"'$piuri' MUST have field 'from' with one element")
                case Some(from) =>
                  Right(
                    DeliveryRequest(
                      id = msg.id,
                      from = from,
                      to = firstTo,
                      limit = body.limit,
                      recipient_did = body.recipient_did,
                    )
                  )
            )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
