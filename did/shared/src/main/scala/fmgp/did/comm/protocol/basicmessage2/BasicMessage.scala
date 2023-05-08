package fmgp.did.comm.protocol.basicmessage2

import zio.json._

import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toBasicMessage: Either[String, BasicMessage] =
    BasicMessage.fromPlaintextMessage(msg)

  /** The Basic Message is sent by the sender to the recipient.
    *
    * Note that the role is only specific to the creation of messages, and that both parties may play both roles.
    *
    * {{{
    *  {
    * "id": "123456780",
    * "type": "https://didcomm.org/basicmessage/2.0/message",
    * "lang": "en",
    * "created_time": 1547577721,
    * "body": {
    * "content": "Your hovercraft is full of eels."
    * }
    * }
    * }}}
    *
    * @param lang
    *   See [https://identity.foundation/didcomm-messaging/spec/#internationalization-i18n]
    */
final case class BasicMessage(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: Option[FROM],
    lang: NotRequired[String] = None,
    created_time: NotRequired[UTCEpoch] = None,
    content: String,
) {
  def `type` = BasicMessage.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      from = from,
      created_time = created_time,
      body = Some(BasicMessage.Body(content).toJSON_RFC7159),
      // FIXME lang: NotRequired[String] = lang,
    )

}

object BasicMessage {
  def piuri = PIURI("https://didcomm.org/basicmessage/2.0/message")

  protected final case class Body(content: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, BasicMessage] = {
    if (msg.`type` != piuri)
      Left(s"No able to create BasicMessage from a Message of the type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case tos =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].map { body =>
                BasicMessage(
                  id = msg.id,
                  to = tos.toSet,
                  from = msg.from,
                  lang = None,
                  created_time = msg.created_time,
                  content = body.content
                )
              }

  }
}
