package fmgp.did.comm.extension

import zio.json._
import fmgp.did.DIDSubject

// Extension: https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/advanced_sequencing/main.md

/** The sender_order header tells other parties how the sender of a message perceives their own ordering. Its value is a
  * monotonically increasing natural/ordinal number (1..N) that tells how many different messages the sender has sent in
  * the current thread. When Alice and Bob are both bidding in an auction protocol, each of them marks their first bid
  * with sender_order: 1, their second bid with sender_order: 2, and so forth. This allows the auctioneer to detect if
  * Alice's bids arrive in a different order than she intended. It also means that any message can be uniquely
  * identified by its thid, the sender, and the value of that sender's sender_order. Note how this does NOT clarify the
  * sequence of Alice's messages relative to Bob's.
  *
  * @see
  *   https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/advanced_sequencing/main.md#sender_order
  */
opaque type SenderOrder = Int
object SenderOrder:
  def apply(): SenderOrder = 1
  def apply(value: Int): SenderOrder = value
  extension (senderOrder: SenderOrder) def value: Int = senderOrder
  given decoder: JsonDecoder[SenderOrder] = JsonDecoder.int.map(SenderOrder(_))
  given encoder: JsonEncoder[SenderOrder] = JsonEncoder.int.contramap[SenderOrder](_.value)

  /** The sent_count header enables resend behavior. A resent message contains the same headers (including id and
    * sender_order) and body as a previous transmission. The first time a message is sent, the sent_count for that
    * message is 1, and the header is normally omitted (the value of the header is implicitly 1). The second time the
    * message is sent, the sent_count is 2, and the header is added to indicate that the message might be redundant
    * ("this is the second time I've sent this to you"). The header continues to be incremented with each subsequent
    * resend. Once a recipient has received one copy of a message, they MUST ignore subsequent copies that arrive, such
    * that resends are idempotent.
    *
    * @see
    *   https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/advanced_sequencing/main.md#sent_count
    */
opaque type SentCount = Int
object SentCount:
  def apply(): SentCount = 1
  def apply(value: Int): SentCount = value
  extension (sentCount: SentCount) def value: Int = sentCount
  given decoder: JsonDecoder[SentCount] = JsonDecoder.int.map(SentCount(_))
  given encoder: JsonEncoder[SentCount] = JsonEncoder.int.contramap[SentCount](_.value)

/** The received_orders header tells other parties how the sender has experienced the unfolding interaction so far. This
  * allows gaps to be detected, possibly triggering a resend. The value of this header is an array of gap detector
  * objects in the format {"id":<did of party>, "last":<value of biggest sender_order seen from this party>, "gaps":
  * []}. In our running auction example, if the auctioneer sees bids 1 and 2 from Bob, and bid 1 and 4 from Alice, the
  * auctioneer might send a message to all the bidders that includes the following received_orders header:
  *
  * {{{
  *  "received_orders": [
  *   {"id": "did:ex:bob", "last": 2, "gaps": []},
  *   {"id": "did:ex:alice", "last": 4, "gaps": [2,3]}
  *   ]
  * }}}
  * This lets Alice (or Bob) notice that the auctioneer hasn't seen the messages that Alice numbered 2 and 3. Gaps can
  * then be plugged or ignored, depending on protocol rules.
  *
  * @see
  *   https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/advanced_sequencing/main.md#received_orders
  */
case class ReceivedOrdersElement(id: DIDSubject, last: SenderOrder, gaps: Seq[SenderOrder])

object ReceivedOrdersElement {
  given decoder: JsonDecoder[ReceivedOrdersElement] = DeriveJsonDecoder.gen[ReceivedOrdersElement]
  given encoder: JsonEncoder[ReceivedOrdersElement] = DeriveJsonEncoder.gen[ReceivedOrdersElement]
}
