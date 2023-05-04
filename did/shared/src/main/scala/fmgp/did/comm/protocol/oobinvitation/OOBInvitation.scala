package fmgp.did.comm.protocol.oobinvitation

import zio.json._

import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toOOBInvitation: Either[String, OOBInvitation] =
    OOBInvitation.fromPlaintextMessage(msg)

  /** The OOB Invitation Message.
    *
    * @see
    *   https://identity.foundation/didcomm-messaging/spec/#invitation
    *
    * {{{
    * {
    *   "id" : "8ce692ff-154b-47ae-869f-2ea90f6df6a7",
    *   "type" : "https://didcomm.org/out-of-band/2.0/invitation",
    *   "from" : "did:example:alices",
    *   "body" : {
    *     "goal_code" : "request-mediate",
    *     "goal" : "RequestMediate",
    *     "accept" : [
    *       "didcomm/v2"
    *     ]
    *   }
    * }
    * }}}
    */
final case class OOBInvitation(
    id: MsgID = MsgID(),
    from: FROM,
    goal_code: NotRequired[String],
    goal: NotRequired[String] = None,
    accept: NotRequired[Seq[String]] = None,
    // TODO attachments: NotRequired[Attachments],
) {
  def `type` = OOBInvitation.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      from = Some(from),
      body = OOBInvitation.Body(goal_code = goal_code, goal = goal, accept = accept).toJSON_RFC7159,
      // FIXME lang: NotRequired[String] = lang,
    )

}

object OOBInvitation {
  def piuri = PIURI("https://didcomm.org/out-of-band/2.0/invitation")

  protected final case class Body(
      goal_code: NotRequired[String],
      goal: NotRequired[String] = None,
      accept: NotRequired[Seq[String]] = None,
  ) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, OOBInvitation] = {
    if (msg.`type` != piuri)
      Left(s"No able to create OOB-Invitation from a Message of the type '${msg.`type`}'")
    else
      msg.from match
        case None => Left(s"'$piuri' MUST have field 'from' with one element")
        case Some(from) =>
          msg.body.as[Body] match
            case Left(value) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
            case Right(body) =>
              Right(
                OOBInvitation(
                  id = msg.id,
                  from = from,
                  goal_code = body.goal_code,
                  goal = body.goal,
                  accept = body.accept,
                )
              )

  }
}
