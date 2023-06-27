package io.iohk.atala.mediator.comm

import zio.json.*
import fmgp.did.comm.*
final case class TapMessage(msg: EncryptedMessage, decrypted: PlaintextMessage)

object TapMessage {
  given decoder: JsonDecoder[TapMessage] = DeriveJsonDecoder.gen[TapMessage]
  given encoder: JsonEncoder[TapMessage] = DeriveJsonEncoder.gen[TapMessage]
}
