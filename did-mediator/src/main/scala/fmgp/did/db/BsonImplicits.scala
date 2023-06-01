package fmgp.did.db

import reactivemongo.api.bson._
import fmgp.did.comm._
import fmgp.did._
import fmgp.util._
import scala.util._

implicit object CipherTextWriter extends BSONWriter[CipherText] {
  import CipherText._
  def writeTry(obj: CipherText): Try[BSONValue] =
    Try(BSONString(obj.value))
}
implicit object TAGWriter extends BSONWriter[TAG] {
  import TAG._
  def writeTry(obj: TAG): Try[BSONValue] =
    Try(BSONString(obj.value))
}
implicit object IVWriter extends BSONWriter[IV] {
  import IV._
  def writeTry(obj: IV): Try[BSONValue] =
    Try(BSONString(obj.value))
}

implicit object Base64ObjWriter extends BSONWriter[Base64Obj[ProtectedHeader]] {
  import Base64Obj._
  def writeTry(obj: Base64Obj[ProtectedHeader]): Try[BSONValue] =
    Try(BSONString(obj.base64url))
}

implicit object Base64Writer extends BSONWriter[Base64] {
  import Base64._
  def writeTry(obj: Base64): Try[BSONValue] =
    Try(BSONString(obj.urlBase64)) // TODO duble check
}

implicit val verificationMethodReferencedWriter: BSONDocumentWriter[VerificationMethodReferenced] =
  Macros.writer[VerificationMethodReferenced]
implicit val recipientHeaderWriter: BSONDocumentWriter[RecipientHeader] = Macros.writer[RecipientHeader]

implicit object RecipientWriter extends BSONWriter[Recipient] {
  import Recipient._
  def writeTry(obj: Recipient): Try[BSONValue] =
    Try(
      BSONDocument(
        "encrypted_key" -> obj.encrypted_key,
        "header" -> obj.header,
      )
    )

  // reactivemongo.api.bson.ElementProducer
}

implicit object encryptedMessageWriter2 extends BSONDocumentWriter[EncryptedMessage] {
  val aux = Macros.writer[EncryptedMessageGeneric]
  override def writeTry(obj: EncryptedMessage): Try[BSONDocument] =
    obj match {
      case msg: EncryptedMessageGeneric => aux.writeTry(msg) // Success(msg): Try[reactivemongo.api.bson.BSONDocument]
      case _                            => Failure(RuntimeException("Only support EncryptedMessageGeneric"))
    }

}
