package fmgp.did.comm

import zio.json._
import zio.json.ast.Json
import fmgp.did._

case class PlaintextMessageClass(
    id: Required[MsgID],
    `type`: PIURI,
    to: NotRequired[Set[DIDSubject]] = None,
    from: NotRequired[DIDSubject] = None,
    thid: NotRequired[MsgID] = None,
    created_time: NotRequired[UTCEpoch] = None,
    expires_time: NotRequired[UTCEpoch] = None,
    body: Required[JSON_RFC7159] = JSON_RFC7159(),
    attachments: NotRequired[Seq[Attachment]] = None,
) extends PlaintextMessage

object PlaintextMessageClass {
  given decoder: JsonDecoder[PlaintextMessageClass] = DeriveJsonDecoder.gen[PlaintextMessageClass]
  given encoder: JsonEncoder[PlaintextMessageClass] = DeriveJsonEncoder.gen[PlaintextMessageClass]
}
