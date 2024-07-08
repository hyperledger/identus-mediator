package org.hyperledger.identus.mediator.db

import fmgp.did.*
import fmgp.did.comm.*
import reactivemongo.api.bson.*
import java.time.Instant
import scala.util.Try
import zio.json._
import scala.util.Failure
import scala.util.Success

type HASH = String
// messages
type XRequestID = String // x-request-id

enum MessageType {
  case Mediator, User
}

case class MessageItem(
    _id: HASH,
    msg: SignedMessage | EncryptedMessage,
    headers: ProtectedHeader | Seq[SignProtectedHeader],
    ts: Instant,
    message_type: MessageType,
    xRequestId: Option[XRequestID]
)
object MessageItem {
  def apply(
      msg: SignedMessage | EncryptedMessage,
      messageType: MessageType,
      xRequestId: Option[XRequestID]
  ): MessageItem =
    val now = Instant.now()
    msg match {
      case sMsg: SignedMessage =>
        new MessageItem(
          msg.sha256,
          msg,
          sMsg.signatures.map(_.`protected`.obj),
          now,
          messageType,
          xRequestId
        )
      case eMsg: EncryptedMessage =>
        new MessageItem(msg.sha256, msg, eMsg.`protected`.obj, now, messageType, xRequestId)
    }

  given BSONWriter[ProtectedHeader | Seq[SignProtectedHeader]] with {
    override def writeTry(obj: ProtectedHeader | Seq[SignProtectedHeader]): Try[BSONValue] =
      obj match {
        case obj: ProtectedHeader =>
          summon[BSONDocumentWriter[ProtectedHeader]].writeTry(obj)
        case seq: Seq[_] =>
          val f = summon[BSONDocumentWriter[SignProtectedHeader]]
          seq
            .map(e => Try(e.asInstanceOf[SignProtectedHeader]).flatMap(ee => f.writeTry(ee)))
            .foldLeft(Try(Seq.empty[BSONDocument]))((acc, e) =>
              (acc, e) match
                case (Failure(exception), _)            => Failure(exception)
                case (Success(seq), Failure(exception)) => Failure(exception)
                case (Success(seq), Success(value))     => Success(seq :+ value)
            )
            .map(e => BSONArray(e))
      }
  }

  given BSONReader[ProtectedHeader | Seq[SignProtectedHeader]] with {
    override def readTry(bson: BSONValue): Try[ProtectedHeader | Seq[SignProtectedHeader]] = {
      bson match
        case array: BSONArray =>
          val f = summon[BSONDocumentReader[SignProtectedHeader]]
          array.values
            .map(e => f.readTry(e))
            .foldLeft(Try(Seq.empty[SignProtectedHeader]))((acc, e) =>
              (acc, e) match
                case (Failure(exception), _)            => Failure(exception)
                case (Success(seq), Failure(exception)) => Failure(exception)
                case (Success(seq), Success(value))     => Success(seq :+ value)
            )
        case obj: BSONDocument => summon[BSONDocumentReader[ProtectedHeader]].readTry(obj)
        case _                 => Failure(new RuntimeException("Must be a Document for a Array of Documents"))
    }
  }

  given BSONWriter[MessageType] with
    def writeTry(value: MessageType): Try[BSONValue] = Try {
      value match {
        case MessageType.Mediator => BSONString("Mediator")
        case MessageType.User     => BSONString("User")
      }
    }

  given BSONReader[MessageType] with
    def readTry(bson: BSONValue): Try[MessageType] = Try {
      bson match {
        case BSONString("Mediator") => MessageType.Mediator
        case BSONString("User")     => MessageType.User
        case _                      => throw new RuntimeException("Invalid MessagePurpose value in BSON")
      }
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
          hash = sMsg.sha256,
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
          hash = eMsg.sha256,
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
