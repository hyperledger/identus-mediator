package io.iohk.atala.mediator.db

import fmgp.did.*
import fmgp.did.comm.*
import reactivemongo.api.bson.*

import java.time.Instant
type HASH = String
// messages

case class MessageItem(_id: HASH, msg: EncryptedMessage, headers: ProtectedHeader)
object MessageItem {
  def apply(msg: EncryptedMessage): MessageItem = {
    new MessageItem(msg.sha1, msg, msg.`protected`.obj)
  }
  given BSONDocumentWriter[MessageItem] = Macros.writer[MessageItem]
  given BSONDocumentReader[MessageItem] = Macros.reader[MessageItem]
}

case class MessageMetaData(hash: HASH, recipient: DIDSubject, state: Boolean, ts: String)
object MessageMetaData {
  given BSONDocumentWriter[MessageMetaData] = Macros.writer[MessageMetaData]
  given BSONDocumentReader[MessageMetaData] = Macros.reader[MessageMetaData]
  def apply(hash: HASH, recipient: DIDSubject) = {
    new MessageMetaData(hash = hash, recipient = recipient, state = false, ts = Instant.now().toString)
  }
}

// did_account did
case class DidAccount(
    _id: BSONObjectID = BSONObjectID.generate(),
    did: DIDSubject,
    alias: Seq[DID],
    messagesRef: Seq[MessageMetaData],
) {
  def queryConditionToInsert = BSONDocument(
    Seq(
      "$or" -> BSONArray(
        BSONDocument(Seq("did" -> BSONString(did.did))),
        BSONDocument(Seq("alias" -> BSONString(did.did))) // TODO test
      )
    )
  )
}

object DidAccount {
  given BSONDocumentWriter[DidAccount] = Macros.writer[DidAccount]
  given BSONDocumentReader[DidAccount] = Macros.reader[DidAccount]
}
