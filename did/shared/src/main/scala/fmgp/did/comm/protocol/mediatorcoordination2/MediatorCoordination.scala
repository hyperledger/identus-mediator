package fmgp.did.comm.protocol.mediatorcoordination2
/* https://didcomm.org/mediator-coordination/2.0/ */

import zio.json._
import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toMediateRequest: Either[String, MediateRequest] = MediateRequest.fromPlaintextMessage(msg)
  def toMediateDeny: Either[String, MediateDeny] = MediateDeny.fromPlaintextMessage(msg)
  def toMediateGrant: Either[String, MediateGrant] = MediateGrant.fromPlaintextMessage(msg)

/** This message serves as a request from the recipient to the mediator, asking for the permission (and routing
  * information) to publish the endpoint as a mediator.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/2.0/mediate-request",
  * }
  * }}}
  */
final case class MediateRequest(id: MsgID = MsgID(), from: FROM, to: TO) {
  def piuri = MediateRequest.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
    )
  def makeRespondMediateGrant = MediateGrant(thid = id, to = from.asTO, from = to.asFROM, Seq(to.asFROMTO))
  def makeRespondMediateDeny = MediateDeny(thid = id, to = from.asTO, from = to.asFROM)
}
object MediateRequest {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/mediate-request")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MediateRequest] =
    if (msg.`type` != piuri) Left(s"No able to create MediateDeny from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from' with one element")
            case Some(from) =>
              Right(
                MediateRequest(
                  id = msg.id,
                  from = from,
                  to = firstTo,
                )
              )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

/** This message serves as notification of the mediator denying the recipient's request for mediation.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/2.0/mediate-deny",
  * }
  * }}}
  */
final case class MediateDeny(id: MsgID = MsgID(), thid: MsgID, from: FROM, to: TO) {
  def piuri = MediateDeny.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
    )
}
object MediateDeny {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/mediate-deny")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MediateDeny] =
    if (msg.`type` != piuri) Left(s"No able to create MediateDeny from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.thid match
            case None => Left(s"'$piuri' MUST have field 'thid'")
            case Some(thid) =>
              msg.from match
                case None => Left(s"'$piuri' MUST have field 'from' with one element")
                case Some(from) =>
                  Right(
                    MediateDeny(
                      id = msg.id,
                      thid = thid,
                      from = from,
                      to = firstTo,
                    )
                  )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

/** A mediate grant message is a signal from the mediator to the recipient that permission is given to distribute the
  * included information as an inbound route.
  *
  * @param routing_did
  *   DID of the mediator where forwarded messages should be sent. The recipient may use this DID as an enpoint as
  *   explained in Using a DID as an endpoint section of the specification.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/2.0/mediate-grant",
  *   "body": {"routing_did": ["did:peer:z6Mkfriq1MqLBoPWecGoDLjguo1sB9brj6wT3qZ5BxkKpuP6"]}
  * }
  * }}}
  */
final case class MediateGrant(id: MsgID = MsgID(), thid: MsgID, from: FROM, to: TO, routing_did: Seq[FROMTO]) {
  def piuri = MediateGrant.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
      body = Some(MediateGrant.Body(routing_did).toJSON_RFC7159)
    )
}
object MediateGrant {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/mediate-grant")
  protected final case class Body(routing_did: Seq[FROMTO]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MediateGrant] =
    if (msg.`type` != piuri) Left(s"No able to create MediateGrant from a Message of type '${msg.`type`}'")
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
                          MediateGrant(
                            id = msg.id,
                            thid = thid,
                            from = from,
                            to = firstTo,
                            routing_did = body.routing_did
                          )
                        )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

//TODO
object KeylistUpdate {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-update")
}

//TODO
object KeylistResponse {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-update-response")
}

//TODO
object KeylistQuery {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-query")
}

//TODO
object Keylist {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist")
}
