package fmgp.did.comm

import zio.json._

final case class TapMessage(msg: EncryptedMessage, decrypted: PlaintextMessage)

object TapMessage {
  given decoder: JsonDecoder[TapMessage] = DeriveJsonDecoder.gen[TapMessage]
  given encoder: JsonEncoder[TapMessage] = DeriveJsonEncoder.gen[TapMessage]
}
