package fmgp.did.comm.protocol.mediatorcoordination2

import zio.json._
import fmgp.did._
import fmgp.did.comm._

//TOOD
extension (msg: PlaintextMessage)
  def toKeylistUpdate: Either[String, KeylistUpdate] = KeylistUpdate.fromPlaintextMessage(msg)
  def toKeylistResponse: Either[String, KeylistResponse] = KeylistResponse.fromPlaintextMessage(msg)
  def toKeylistQuery: Either[String, KeylistQuery] = ??? // KeylistQuery.fromPlaintextMessage(msg)
  def toKeylist: Either[String, Keylist] = ??? // Keylist.fromPlaintextMessage(msg)

enum KeylistAction:
  case add extends KeylistAction
  case remove extends KeylistAction

object KeylistAction {
  given decoder: JsonDecoder[KeylistAction] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(KeylistAction.valueOf(e)))
  given encoder: JsonEncoder[KeylistAction] = JsonEncoder.string.contramap((e: KeylistAction) => e.toString)
}

enum KeylistResult:
  case client_error extends KeylistResult
  case server_error extends KeylistResult
  case no_change extends KeylistResult
  case success extends KeylistResult

object KeylistResult:
  given decoder: JsonDecoder[KeylistResult] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(KeylistResult.valueOf(e)))
  given encoder: JsonEncoder[KeylistResult] = JsonEncoder.string.contramap((e: KeylistResult) => e.toString)

final case class KeylistUpdate(id: MsgID = MsgID(), from: FROM, to: TO, updates: Seq[(FROMTO, KeylistAction)]) {
  def piuri = KeylistUpdate.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        KeylistUpdate
          .Body(updates =
            updates.map(e =>
              KeylistUpdate.Update(
                recipient_did = e._1,
                action = e._2
              )
            )
          )
          .toJSON_RFC7159
      ),
    )
  def makeKeylistResponse(updated: Seq[(FROMTO, KeylistAction, KeylistResult)]) =
    KeylistResponse(thid = id, to = from.asTO, from = to.asFROM, updated)
}

/** TODO we don't believe this behavior is correct or secure. But ismimic the behavior of RootsID mediator
  *
  * https://identity.foundation/didcomm-messaging/spec/#routing-protocol-20:~:text=rfc587%22%0A%20%20%[…]le%3Asomemediator%23somekey%22%5D,-%7D%5D%0A%7D
  */
object KeylistUpdate {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-update")

  protected final case class Update(recipient_did: FROMTO, action: KeylistAction) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Update {
    given decoder: JsonDecoder[Update] = DeriveJsonDecoder.gen[Update]
    given encoder: JsonEncoder[Update] = DeriveJsonEncoder.gen[Update]
  }

  protected final case class Body(updates: Seq[Update]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, KeylistUpdate] =
    if (msg.`type` != piuri) Left(s"No able to create KeylistUpdate from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from' with one element")
            case Some(from) =>
              msg.body.map(_.as[Body]) match
                case None              => Left(s"'$piuri' MUST have a 'body'")
                case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                case Some(Right(body)) =>
                  Right(
                    KeylistUpdate(
                      id = msg.id,
                      from = from,
                      to = firstTo,
                      updates = body.updates.map(e => (e.recipient_did, e.action))
                    )
                  )

        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

final case class KeylistResponse(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    updated: Seq[(FROMTO, KeylistAction, KeylistResult)]
) {
  def piuri = KeylistResponse.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      thid = Some(thid),
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        KeylistResponse
          .Body(updated =
            updated.map(e =>
              KeylistResponse.Updated(
                routing_did = e._1,
                action = e._2,
                result = e._3
              )
            )
          )
          .toJSON_RFC7159
      ),
    )
}

object KeylistResponse {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-update-response")

  protected final case class Updated(routing_did: FROMTO, action: KeylistAction, result: KeylistResult) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Updated {
    given decoder: JsonDecoder[Updated] = DeriveJsonDecoder.gen[Updated]
    given encoder: JsonEncoder[Updated] = DeriveJsonEncoder.gen[Updated]
  }

  protected final case class Body(updated: Seq[Updated]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, KeylistResponse] =
    if (msg.`type` != piuri) Left(s"No able to create KeylistResponse from a Message of type '${msg.`type`}'")
    else
      msg.thid match
        case None => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
            case firstTo +: Seq() =>
              msg.from match
                case None => Left(s"'$piuri' MUST have field 'from' with one element")
                case Some(from) =>
                  msg.body.map(_.as[Body]) match
                    case None              => Left(s"'$piuri' MUST have a 'body'")
                    case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                    case Some(Right(body)) =>
                      Right(
                        KeylistResponse(
                          id = msg.id,
                          thid = thid,
                          from = from,
                          to = firstTo,
                          updated = body.updated.map(e => (e.routing_did, e.action, e.result))
                        )
                      )

            case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

final case class KeylistQuery(id: MsgID = MsgID(), from: FROM, to: TO)
//TODO
object KeylistQuery {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-query")
}

final case class Keylist(id: MsgID = MsgID(), from: FROM, to: TO)
//TODO
object Keylist {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist")
}
