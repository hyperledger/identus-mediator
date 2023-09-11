package io.iohk.atala.mediator.db

import fmgp.did.*
import fmgp.did.comm.*
import reactivemongo.api.bson.*
import java.time.Instant
import scala.util.Try
import zio.json._

type HASH = String
// messages
type XRequestID = String // x-request-id

case class MessageItem(
    _id: HASH,
    msg: EncryptedMessage,
    headers: ProtectedHeader,
    ts: String,
    xRequestId: Option[XRequestID]
)
object MessageItem {
  def apply(msg: EncryptedMessage, xRequestId: Option[XRequestID]): MessageItem = {
    new MessageItem(msg.sha1, msg, msg.`protected`.obj, Instant.now().toString, xRequestId)
  }
  given BSONDocumentWriter[MessageItem] = Macros.writer[MessageItem]
  given BSONDocumentReader[MessageItem] = Macros.reader[MessageItem]
}

case class MessageMetaData(
    hash: HASH,
    recipient: DIDSubject,
    state: Boolean,
    ts: String,
    xRequestId: Option[XRequestID]
)
object MessageMetaData {
  given BSONDocumentWriter[MessageMetaData] = Macros.writer[MessageMetaData]
  given BSONDocumentReader[MessageMetaData] = Macros.reader[MessageMetaData]
  def apply(hash: HASH, recipient: DIDSubject, xRequestId: Option[XRequestID]): MessageMetaData = {
    new MessageMetaData(hash = hash, recipient = recipient, state = false, ts = Instant.now().toString, xRequestId)
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
    encrypt: SignedMessage | EncryptedMessage,
    hash: HASH,
    headers: ast.Json, // ProtectedHeader | SignProtectedHeader,
    plaintext: PlaintextMessage,
    transport: Seq[SentMessageItem.TransportInfo],
)

object SentMessageItem {

  def apply(
      msg: SignedMessage | EncryptedMessage,
      plaintext: PlaintextMessage,
      recipient: Set[TO],
      distination: Option[String],
      sendMethod: MessageSendMethod,
      result: Option[String],
      xRequestId: Option[XRequestID]
  ): SentMessageItem = {
    msg match
      case sMsg: SignedMessage =>
        new SentMessageItem(
          encrypt = msg,
          hash = sMsg.sha1, // FIXME
          headers = sMsg.signatures.headOption.flatMap(_.`protected`.obj.toJsonAST.toOption).getOrElse(ast.Json.Null),
          plaintext = plaintext,
          transport = Seq(
            TransportInfo(
              recipient = recipient,
              distination = distination,
              sendMethod = sendMethod,
              result = result,
              xRequestId = xRequestId
            )
          )
        )
      case eMsg: EncryptedMessage =>
        new SentMessageItem(
          encrypt = msg,
          hash = eMsg.sha1,
          headers = eMsg.`protected`.obj.toJsonAST.getOrElse(ast.Json.Null),
          plaintext = plaintext,
          transport = Seq(
            TransportInfo(
              recipient = recipient,
              distination = distination,
              sendMethod = sendMethod,
              result = result,
              xRequestId = xRequestId
            )
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
      xRequestId: Option[XRequestID]
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
