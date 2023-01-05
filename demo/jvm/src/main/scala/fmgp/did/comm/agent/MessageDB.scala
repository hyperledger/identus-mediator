package fmgp.did.comm.mediator

import fmgp.did.comm.EncryptedMessage
import scala.collection.immutable.HashMap

type HashEncryptedMessage = Int

case class MsgContex(
    hash: HashEncryptedMessage,
)

final case class MessageDB(
    db: Map[HashEncryptedMessage, EncryptedMessage] = Map.empty,
    ctx: Map[HashEncryptedMessage, MsgContex] = Map.empty,
) {
  def add(msg: EncryptedMessage): MessageDB = {
    this.copy(db = db + (msg.hashCode -> msg))
  }
}
