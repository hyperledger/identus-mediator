package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.did.comm.extension._

import fmgp.did.comm.ReturnRoute
case class PlaintextMessageClass(
    id: Required[MsgID],
    `type`: PIURI,
    to: NotRequired[Set[TO]] = None,
    from: NotRequired[FROM] = None,
    thid: NotRequired[MsgID] = None,
    created_time: NotRequired[UTCEpoch] = None,
    expires_time: NotRequired[UTCEpoch] = None,
    body: NotRequired[JSON_RFC7159] = Some( // TODO change to None in Future
      JSON_RFC7159()
    ), // 'Some' is for compatibility with DIDComm v2.0
    attachments: NotRequired[Seq[Attachment]] = None,

    // # Extensions
    // DID rotation
    from_prior: NotRequired[JWTToken] = None,

    // Return Route Header
    return_route: NotRequired[ReturnRoute] = None,

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
