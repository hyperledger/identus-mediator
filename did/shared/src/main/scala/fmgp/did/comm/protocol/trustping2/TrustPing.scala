/** https://identity.foundation/didcomm-messaging/spec/#trust-ping-protocol-20 */
package fmgp.did.comm.protocol.trustping2

import zio.json._
import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toTrustPing: Either[String, TrustPing] =
    TrustPing.fromPlaintextMessage(msg)
  def toTrustPingResponse: Either[String, TrustPingResponse] =
    TrustPingResponse.fromPlaintextMessage(msg)

/** https://didcomm.org/trust-ping/2.0/ping
  *
  * {{{
  * {
  * "type": "https://didcomm.org/trust-ping/2.0/ping",
  * "id": "518be002-de8e-456e-b3d5-8fe472477a86",
  * "from": "did:example:123456",
  * "body": {"response_requested": true}
  * }
  * }}}
  */
sealed trait TrustPing {
  def piuri = TrustPing.piuri
  def id: MsgID
  def to: TO
  def response_requested: Boolean // default is false

  // utils
  def toPlaintextMessage: PlaintextMessage
}

object TrustPing {
  def piuri = PIURI("https://didcomm.org/trust-ping/2.0/ping")
  protected final case class Body(response_requested: Option[Boolean]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected[trustping2] object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, TrustPing] =
    if (msg.`type` != piuri) Left(s"No able to create TrustPing from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: tail =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap {
                case Body(None) | Body(Some(false)) =>
                  Right(new TrustPingWithOutRequestedResponse(id = msg.id, to = firstTo, from = msg.from.map(e => e)))
                case Body(Some(true)) =>
                  msg.from match
                    case None => Left(s"'$piuri' MUST have field 'from' with one element if response_requested is true")
                    case Some(from) => Right(new TrustPingWithRequestedResponse(id = msg.id, from = from, to = firstTo))
              }

}

final case class TrustPingWithRequestedResponse(
    id: MsgID = MsgID(),
    from: FROM,
    to: TO,
) extends TrustPing {
  def response_requested: Boolean = true

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(TrustPing.Body(Some(response_requested)).toJSON_RFC7159),
    )

  def makeRespond = TrustPingResponse(thid = id, to = from.asTO, from = Some(to.asFROM))
}

final case class TrustPingWithOutRequestedResponse(
    id: MsgID = MsgID(),
    from: Option[FROM],
    to: TO,
) extends TrustPing {
  def response_requested: Boolean = false

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = from.map(e => e),
      body = Some(TrustPing.Body(Some(response_requested)).toJSON_RFC7159),
    )

}

/** {{{
  * {
  * "type": "https://didcomm.org/trust-ping/2.0/ping-response",
  * "id": "e002518b-456e-b3d5-de8e-7a86fe472847",
  * "thid": "518be002-de8e-456e-b3d5-8fe472477a86"
  * }
  * }}}
  */
final case class TrustPingResponse(
    id: MsgID = MsgID(),
    thid: MsgID,
    to: TO,
    from: Option[FROM],
) {
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = TrustPingResponse.piuri,
      thid = Some(thid),
      to = Some(Set(to)),
      from = from
    )

}
object TrustPingResponse {
  def piuri = PIURI("https://didcomm.org/trust-ping/2.0/ping-response")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, TrustPingResponse] =
    if (msg.`type` != piuri)
      Left(s"No able to create TrustPingResponse from a Message of the type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.thid match
            case None => Left(s"'$piuri' MUST have the field 'thid'")
            case Some(thid) =>
              Right(
                TrustPingResponse(
                  id = msg.id,
                  thid = thid,
                  to = firstTo,
                  from = msg.from,
                )
              )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
