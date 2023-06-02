package fmgp.did.db

import reactivemongo.api.bson._
import fmgp.did.comm._
import fmgp.did._
import fmgp.util._
import scala.util._
import zio.json._

given BSONWriter[DIDSubject] with {
  import DIDSubject._
  def writeTry(obj: DIDSubject): Try[BSONValue] = Try(BSONString(obj.string))
}

given BSONReader[DIDSubject] with {
  def readTry(bson: BSONValue): Try[DIDSubject] = bson.asTry[String].map(v => DIDSubject(v))
}

given BSONWriter[DID] with {
  import DID._
  def writeTry(obj: DID): Try[BSONValue] = Try(BSONString(obj.string))
}

given BSONReader[DID] with {
  def readTry(bson: BSONValue): Try[DID] = bson.asTry[String].map(v => DIDSubject(v))
}

given BSONWriter[CipherText] with {
  import CipherText._
  def writeTry(obj: CipherText): Try[BSONValue] = Try(BSONString(obj.value))
}

given BSONReader[CipherText] with {
  def readTry(bson: BSONValue): Try[CipherText] = bson.asTry[String].map(v => CipherText(v))
}

given BSONWriter[TAG] with {
  import TAG._
  def writeTry(obj: TAG): Try[BSONValue] = Try(BSONString(obj.value))
}
given BSONReader[TAG] with {
  def readTry(bson: BSONValue): Try[TAG] = bson.asTry[String].map(v => TAG(v))
}
given BSONWriter[IV] with {
  import IV._
  def writeTry(obj: IV): Try[BSONValue] = Try(BSONString(obj.value))
}
given BSONReader[IV] with {
  def readTry(bson: BSONValue): Try[IV] = bson.asTry[String].map(v => IV(v))
}

given BSONWriter[Base64Obj[ProtectedHeader]] with {
  import Base64Obj._
  def writeTry(obj: Base64Obj[ProtectedHeader]): Try[BSONValue] = Try(BSONString(obj.base64url))
}
given BSONReader[Base64Obj[ProtectedHeader]] with {
  def readTry(bson: BSONValue): Try[Base64Obj[ProtectedHeader]] =
    bson
      .asTry[String]
      .flatMap(v =>
        v.fromJson[Base64Obj[ProtectedHeader]] match
          case Left(value)  => Failure(RuntimeException(value))
          case Right(value) => Try(value)
      )
}

given BSONWriter[Base64] with {
  import Base64._
  def writeTry(obj: Base64): Try[BSONValue] = Try(BSONString(obj.urlBase64))
}

given BSONReader[Base64] with {
  import Base64._
  def readTry(bson: BSONValue): Try[Base64] = bson.asTry[String].map(v => Base64.fromBase64url(v))
}

given BSONDocumentWriter[Recipient] =
  Macros.writer[Recipient]
given BSONDocumentReader[Recipient] =
  Macros.reader[Recipient]

given BSONDocumentWriter[VerificationMethodReferenced] =
  Macros.writer[VerificationMethodReferenced]
given BSONDocumentReader[VerificationMethodReferenced] =
  Macros.reader[VerificationMethodReferenced]

given BSONDocumentWriter[RecipientHeader] =
  Macros.writer[RecipientHeader]
given BSONDocumentReader[RecipientHeader] =
  Macros.reader[RecipientHeader]

given BSONDocumentWriter[EncryptedMessage] with {
  val aux = Macros.writer[EncryptedMessageGeneric]
  override def writeTry(obj: EncryptedMessage): Try[BSONDocument] =
    obj match {
      case msg: EncryptedMessageGeneric => aux.writeTry(msg) // Success(msg): Try[reactivemongo.api.bson.BSONDocument]
      case _                            => Failure(RuntimeException("Only support EncryptedMessageGeneric"))
    }

}
given BSONDocumentReader[EncryptedMessage] with {
  val aux = Macros.reader[EncryptedMessageGeneric]
  override def readDocument(doc: BSONDocument): Try[EncryptedMessage] =
    aux.readDocument(doc)
}
