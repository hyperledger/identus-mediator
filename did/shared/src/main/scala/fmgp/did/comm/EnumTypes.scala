package fmgp.did.comm

import zio.json._

/** https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/return_route/main.md
  */
enum ReturnRoute:
  /** Send all messages for this DID over the connection.
    *
    * For HTTP transports, the presence of this message decorator indicates that the receiving agent MAY hold onto the
    * connection and use it to return messages as designated. HTTP transports will only be able to receive at most one
    * message at a time. Websocket transports are capable of receiving multiple messages over a single connection.
    */
  case all extends ReturnRoute

  /** Send all messages matching the DID and thread specified in the return_route_thread attribute. */
  case thread extends ReturnRoute

object ReturnRoute {
  given decoder: JsonDecoder[ReturnRoute] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(ReturnRoute.valueOf(e)))
  given encoder: JsonEncoder[ReturnRoute] =
    JsonEncoder.string.contramap((e: ReturnRoute) => e.toString)
}
