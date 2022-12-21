package fmgp.did.comm

import zio.json._
import zio.json.ast.Json
import fmgp.did._

case class PlaintextMessageClass(
    id: Required[String],
    `type`: PIURI,
    to: NotRequired[Set[DIDSubject]],
    from: NotRequired[DIDSubject],
    thid: NotRequired[String],
    created_time: NotRequired[UTCEpoch],
    expires_time: NotRequired[UTCEpoch],
    body: Required[JSON_RFC7159],
    attachments: NotRequired[Seq[Attachment]]
) extends PlaintextMessage

object PlaintextMessageClass {
  given decoder: JsonDecoder[PlaintextMessageClass] = DeriveJsonDecoder.gen[PlaintextMessageClass]
  given encoder: JsonEncoder[PlaintextMessageClass] = DeriveJsonEncoder.gen[PlaintextMessageClass]
}
