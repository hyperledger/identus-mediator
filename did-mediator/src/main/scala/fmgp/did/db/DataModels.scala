package fmgp.did.db

import fmgp.did._
import fmgp.did.comm.EncryptedMessage
import reactivemongo.api.bson._

type HASH = Int
// messages
case class MessageItem(id: HASH, msg: EncryptedMessage)
object MessageItem {
  def apply(msg: EncryptedMessage): MessageItem = new MessageItem(msg.hashCode(), msg)
  given BSONDocumentWriter[MessageItem] = Macros.writer[MessageItem]
}

// clients_store
case class MessageMetaData(id: HASH, state: Boolean, ts: String)
case class ClientStore(id: DIDSubject, alias: Seq[DID], messagesRef: Seq[MessageMetaData])
