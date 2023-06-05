package fmgp.did.db

import fmgp.did._
import fmgp.did.comm.EncryptedMessage
import reactivemongo.api.bson._

type HASH = Int
// messages
case class MessageItem(_id: HASH, msg: EncryptedMessage)
object MessageItem {
  def apply(msg: EncryptedMessage): MessageItem = new MessageItem(msg.hashCode(), msg)
  given BSONDocumentWriter[MessageItem] = Macros.writer[MessageItem]
  given BSONDocumentReader[MessageItem] = Macros.reader[MessageItem]
}

case class MessageMetaData(hash: HASH, state: Boolean, ts: String)
object MessageMetaData {
  given BSONDocumentWriter[MessageMetaData] = Macros.writer[MessageMetaData]
  given BSONDocumentReader[MessageMetaData] = Macros.reader[MessageMetaData]
}

// did_account did
case class DidAccount(
    _id: BSONObjectID = BSONObjectID.generate(),
    did: DIDSubject,
    alias: Seq[DID],
    messagesRef: Seq[MessageMetaData],
)

object DidAccount {
  given BSONDocumentWriter[DidAccount] = Macros.writer[DidAccount]
  given BSONDocumentReader[DidAccount] = Macros.reader[DidAccount]
}
