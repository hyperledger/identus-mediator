package fmgp.webapp

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.extension._
import fmgp.did.comm.protocol.basicmessage2._
import fmgp.did.comm.protocol.mediatorcoordination2._
import fmgp.did.comm.protocol.pickup3._
import fmgp.did.comm.protocol.routing2._
import fmgp.did.comm.protocol.trustping2._
import fmgp.util.Base64

object MessageTemplate {
  def mFrom: Option[FROM] = Global.agentVar.now().flatMap(o => FROM.either(o.id.string).toOption)
  def from: FROM = mFrom.getOrElse(DidExample.senderDIDDocument.id.toDID)
  def to: TO = Global.recipientVar.now().getOrElse(DidExample.recipientDIDDocument.id.toDID)
  def thid: MsgID = MsgID("thid-responding-to-msg-id")

  def exPlaintextMessage = PlaintextMessageClass(
    id = MsgID(),
    `type` = PIURI("basic"),
    to = Some(Set(to)),
    from = Some(from),
    thid = Some(thid),
    created_time = Some(123456789),
    expires_time = Some(123456789),
    body = JSON_RFC7159(),
    attachments = Some(Seq.empty[Attachment]),
    // # Extensions
    return_route = Some(ReturnRoute.all),
    `accept-lang` = Some(Seq("PT")),
    lang = Some("PT"), // IANA’s language codes  // IANA’s language subtag registry.
    // l10n = Some(L10n(
    //   inline = Some(Seq[L10nInline),
    //   service = Some(L10nService),
    //   table = Some(L10nTable)
    //   )),
    // sender_order: NotRequired[SenderOrder] = None,
    // sent_count: NotRequired[SentCount] = None,
    // received_orders: NotRequired[Seq[ReceivedOrdersElement]] = None,
  )

  def exForwardMessageJson = ForwardMessageJson(
    to = Set(to),
    next = to.toDID,
    from = None,
    expires_time = Some(987654321),
    msg = obj_encryptedMessage_ECDHES_X25519_XC20P,
  )
  def exForwardMessageBase64 = ForwardMessageBase64(
    to = Set(to),
    next = to.toDID,
    from = None,
    expires_time = Some(987654321),
    msg = obj_encryptedMessage_ECDHES_X25519_XC20P,
  )

  def exTrustPing = TrustPingWithRequestedResponse(from = from, to = to)
  def exTrustPingResponse = TrustPingResponse(thid = MsgID("some_thid_123"), from = mFrom, to = to)
  def exBasicMessage = BasicMessage(from = mFrom, to = Set(to), content = "Hello, World!")

  def exMediateRequest = MediateRequest(from = from, to = to)
  def exMediateGrant = MediateGrant(from = from, to = to, thid = thid, routing_did = Seq(from.asFROMTO))
  def exMediateDeny = MediateDeny(from = from, to = to, thid = thid)
  // def exKeylistUpdate
  // def exKeylistResponse
  // def exKeylistQuery
  // def exKeylist
  def exStatusRequest = StatusRequest(from = from, to = to, recipient_did = Some(FROMTO("did:recipient_did:123")))
  def exStatus = Status(
    from = from,
    to = to,
    thid = thid,
    recipient_did = Some(FROMTO("did:recipient_did:123")),
    message_count = 5,
    longest_waited_seconds = Some(3600),
    newest_received_time = Some(1658085169),
    oldest_received_time = Some(1658084293),
    total_bytes = Some(8096),
    live_delivery = Some(false),
  )
  def exMessageDelivery = MessageDelivery(
    from = from,
    to = to,
    thid = thid,
    recipient_did = Some(FROMTO("did:recipient_did:123")),
    attachments = Map("321" -> obj_encryptedMessage_ECDHES_X25519_XC20P)
  )
  def exDeliveryRequest =
    DeliveryRequest(from = from, to = to, limit = 5, recipient_did = Some(FROMTO("did:recipient_did:123")))
  def exMessagesReceived = MessagesReceived(from = from, to = to, thid = thid, message_id_list = Seq("321"))
  def exLiveModeChange = LiveModeChange(from = from, to = to, live_delivery = true)

  import fmgp.util._
  import fmgp.crypto._

  val obj_encryptedMessage_ECDHES_X25519_XC20P = EncryptedMessageGeneric(
    ciphertext = CipherText(
      "KWS7gJU7TbyJlcT9dPkCw-ohNigGaHSukR9MUqFM0THbCTCNkY-g5tahBFyszlKIKXs7qOtqzYyWbPou2q77XlAeYs93IhF6NvaIjyNqYklvj-OtJt9W2Pj5CLOMdsR0C30wchGoXd6wEQZY4ttbzpxYznqPmJ0b9KW6ZP-l4_DSRYe9B-1oSWMNmqMPwluKbtguC-riy356Xbu2C9ShfWmpmjz1HyJWQhZfczuwkWWlE63g26FMskIZZd_jGpEhPFHKUXCFwbuiw_Iy3R0BIzmXXdK_w7PZMMPbaxssl2UeJmLQgCAP8j8TukxV96EKa6rGgULvlo7qibjJqsS5j03bnbxkuxwbfyu3OxwgVzFWlyHbUH6p"
    ),
    // {"epk":{"kty":"OKP","crv":"X25519","x":"JHjsmIRZAaB0zRG_wNXLV2rPggF00hdHbW5rj8g0I24"},"apv":"NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA","typ":"application/didcomm-encrypted+json","enc":"XC20P","alg":"ECDH-ES+A256KW"}
    `protected` = Base64Obj[ProtectedHeader](
      AnonProtectedHeader(
        epk = OKPPublicKey(
          kty = KTY.OKP,
          crv = Curve.X25519,
          x = "JHjsmIRZAaB0zRG_wNXLV2rPggF00hdHbW5rj8g0I24",
          kid = None
        ),
        apv = APV("NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA"),
        typ = Some(MediaTypes.ENCRYPTED),
        enc = ENCAlgorithm.XC20P,
        alg = KWAlgorithm.`ECDH-ES+A256KW`,
      ),
      Some(
        Base64(
          "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkpIanNtSVJaQWFCMHpSR193TlhMVjJyUGdnRjAwaGRIYlc1cmo4ZzBJMjQifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0"
        )
      )
    ),
    recipients = Seq(
      Recipient(
        encrypted_key = Base64("3n1olyBR3nY7ZGAprOx-b7wYAKza6cvOYjNwVg3miTnbLwPP_FmE1A"),
        header = RecipientHeader(VerificationMethodReferenced("did:example:bob#key-x25519-1"))
      ),
      Recipient(
        encrypted_key = Base64("j5eSzn3kCrIkhQAWPnEwrFPMW6hG0zF_y37gUvvc5gvlzsuNX4hXrQ"),
        header = RecipientHeader(VerificationMethodReferenced("did:example:bob#key-x25519-2"))
      ),
      Recipient(
        encrypted_key = Base64("TEWlqlq-ao7Lbynf0oZYhxs7ZB39SUWBCK4qjqQqfeItfwmNyDm73A"),
        header = RecipientHeader(VerificationMethodReferenced("did:example:bob#key-x25519-3"))
      ),
    ),
    tag = TAG("6ylC_iAs4JvDQzXeY6MuYQ"),
    iv = IV("ESpmcyGiZpRjc5urDela21TOOTW8Wqd1")
  )
}
