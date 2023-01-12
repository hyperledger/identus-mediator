package fmgp.did.comm

import zio.json._
import zio.json.ast.Json
import fmgp.did._
import fmgp.did.comm.extension._

case class PlaintextMessageClass(
    id: Required[MsgID],
    `type`: PIURI,
    to: NotRequired[Set[TO]] = None,
    from: NotRequired[FROM] = None,
    thid: NotRequired[MsgID] = None,
    created_time: NotRequired[UTCEpoch] = None,
    expires_time: NotRequired[UTCEpoch] = None,
    body: Required[JSON_RFC7159] = JSON_RFC7159(),
    attachments: NotRequired[Seq[Attachment]] = None,

    // # Extensions
    // l10n
    `accept-lang`: NotRequired[Seq[LanguageCodeIANA]] = None,
    lang: NotRequired[LanguageCodeIANA] = None, // IANA’s language codes  // IANA’s language subtag registry.
    l10n: NotRequired[L10n] = None,

    // advanced_sequencing
    sender_order: NotRequired[SenderOrder] = None,
    sent_count: NotRequired[SentCount] = None,
    received_orders: NotRequired[Seq[ReceivedOrdersElement]] = None,
) extends PlaintextMessage

object PlaintextMessageClass {
  given decoder: JsonDecoder[PlaintextMessageClass] = DeriveJsonDecoder.gen[PlaintextMessageClass]
  given encoder: JsonEncoder[PlaintextMessageClass] = DeriveJsonEncoder.gen[PlaintextMessageClass]
}
