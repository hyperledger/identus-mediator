package fmgp.did

import zio.json._

import fmgp.did.comm.EncryptedMessage
import scala.collection.immutable.HashMap

type HashEncryptedMessage = Int

case class MsgContex(
    hash: HashEncryptedMessage,
)

object MsgContex {
  given JsonDecoder[MsgContex] = DeriveJsonDecoder.gen[MsgContex]
  given JsonEncoder[MsgContex] = DeriveJsonEncoder.gen[MsgContex]
}

final case class MessageDB(
    db: Map[HashEncryptedMessage, EncryptedMessage] = Map.empty,
    ctx: Map[HashEncryptedMessage, MsgContex] = Map.empty,
) {
  def add(msg: EncryptedMessage): MessageDB = {
    this.copy(db = db + (msg.hashCode -> msg))
  }
}

object MessageDB {
  given JsonDecoder[MessageDB] = DeriveJsonDecoder.gen[MessageDB]
  given JsonEncoder[MessageDB] = DeriveJsonEncoder.gen[MessageDB]
}
