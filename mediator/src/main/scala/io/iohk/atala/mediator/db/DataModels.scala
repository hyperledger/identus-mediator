package io.iohk.atala.mediator.db

import fmgp.did.*
import fmgp.did.comm.*
import reactivemongo.api.bson.*
import java.time.Instant
import scala.util.Try

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
)

object DidAccount {
  given BSONDocumentWriter[DidAccount] = Macros.writer[DidAccount]
  given BSONDocumentReader[DidAccount] = Macros.reader[DidAccount]
}

// messages outbox
case class SentMessageItem(
    _id: BSONObjectID = BSONObjectID.generate(),
    encrypt: EncryptedMessage,
    hash: HASH,
    headers: ProtectedHeader,
    plaintext: PlaintextMessage,
    transport: Seq[SentMessageItem.TransportInfo],
)

object SentMessageItem {

  def apply(
      msg: EncryptedMessage,
      plaintext: PlaintextMessage,
      recipient: Set[TO],
      distination: Option[String],
      sendMethod: MessageSendMethod,
      result: Option[String]
  ): SentMessageItem = {
    new SentMessageItem(
      encrypt = msg,
      hash = msg.sha1,
      headers = msg.`protected`.obj,
      plaintext = plaintext,
      transport = Seq(
        TransportInfo(recipient = recipient, distination = distination, sendMethod = sendMethod, result = result)
      )
    )
  }

  given BSONDocumentWriter[SentMessageItem] = {
    import SentMessageItem.given_BSONDocumentWriter_TransportInfo
    Macros.writer[SentMessageItem]
  }
  given BSONDocumentReader[SentMessageItem] = {
    import SentMessageItem.given_BSONDocumentReader_TransportInfo
    Macros.reader[SentMessageItem]
  }

  case class TransportInfo(
      recipient: Set[TO],
      distination: Option[String],
      sendMethod: MessageSendMethod,
      timestamp: BSONDateTime = BSONDateTime(Instant.now().toEpochMilli()), // Long,
      result: Option[String],
  )
  object SentMessageItem {
    given BSONDocumentWriter[TransportInfo] = Macros.writer[TransportInfo]
    given BSONDocumentReader[TransportInfo] = Macros.reader[TransportInfo]
  }
}

enum MessageSendMethod {
  case HTTPS_POST extends MessageSendMethod
  case INLINE_REPLY extends MessageSendMethod
}
object MessageSendMethod {
  given BSONWriter[MessageSendMethod] with {
    def writeTry(obj: MessageSendMethod): Try[BSONValue] =
      Try(BSONString(obj.toString))
  }
  given BSONReader[MessageSendMethod] with {
    def readTry(bson: BSONValue): Try[MessageSendMethod] =
      bson.asTry[String].flatMap(v => Try(MessageSendMethod.valueOf(v)))
  }
}
