package fmgp.did

import zio.json._
import fmgp.did.comm.EncryptedMessage
import scala.collection.immutable.HashMap
import fmgp.did.HashEncryptedMessage

object MsgContex {
  given JsonDecoder[MsgContex] = DeriveJsonDecoder.gen[MsgContex]
  given JsonEncoder[MsgContex] = DeriveJsonEncoder.gen[MsgContex]
}

case class MsgContex(
    hash: HashEncryptedMessage,
)
