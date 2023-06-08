package fmgp.did.db

import scala.util._
import zio.json._
import reactivemongo.api.bson._

import fmgp.crypto._
import fmgp.did._
import fmgp.did.comm._
import fmgp.util._

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

given BSONWriter[APV] with {
  import APV._
  def writeTry(obj: APV): Try[BSONValue] = Try(BSONString(obj.value))
}
given BSONReader[APV] with {
  def readTry(bson: BSONValue): Try[APV] = bson.asTry[String].map(v => APV(v))
}

given BSONWriter[APU] with {
  import APU._
  def writeTry(obj: APU): Try[BSONValue] = Try(BSONString(obj.value))
}
given BSONReader[APU] with {
  def readTry(bson: BSONValue): Try[APU] = bson.asTry[String].map(v => APU(v))
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
        ("\"" + v + "\"").fromJson[Base64Obj[ProtectedHeader]] match // TODO with a new methods from ScalaDid
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

given BSONWriter[KTY] with {
  def writeTry(obj: KTY): Try[BSONValue] = Try(BSONString(obj.toString()))
}
given BSONReader[KTY] with {
  def readTry(bson: BSONValue): Try[KTY] = bson.asTry[String].map(v => KTY.valueOf(v))
}

given BSONWriter[Curve] with {
  def writeTry(obj: Curve): Try[BSONValue] = Try(BSONString(obj.toString()))
}
given BSONReader[Curve] with {
  def readTry(bson: BSONValue): Try[Curve] = bson.asTry[String].map(v => Curve.valueOf(v))
}

given BSONWriter[ENCAlgorithm] with {
  def writeTry(obj: ENCAlgorithm): Try[BSONValue] = Try(BSONString(obj.toString()))
}
given BSONReader[ENCAlgorithm] with {
  def readTry(bson: BSONValue): Try[ENCAlgorithm] = bson.asTry[String].map(v => ENCAlgorithm.valueOf(v))
}

given BSONWriter[KWAlgorithm] with {
  def writeTry(obj: KWAlgorithm): Try[BSONValue] = Try(BSONString(obj.toString()))
}
given BSONReader[KWAlgorithm] with {
  def readTry(bson: BSONValue): Try[KWAlgorithm] = bson.asTry[String].map(v => KWAlgorithm.valueOf(v))
}
given BSONWriter[MediaTypes] with {
  def writeTry(obj: MediaTypes): Try[BSONValue] = Try(BSONString(obj.toString()))
}
given BSONReader[MediaTypes] with {
  def readTry(bson: BSONValue): Try[MediaTypes] = bson.asTry[String].map(v => MediaTypes.valueOf(v))
}

given BSONDocumentWriter[PublicKey] with {
  override def writeTry(obj: PublicKey): Try[BSONDocument] =
    obj match {
      case ECPublicKey(kty, crv, x, y, kid) =>
        Try(
          BSONDocument(
            "kty" -> kty,
            "crv" -> crv,
            "x" -> x,
            "y" -> y,
            "kid" -> kid,
          )
        )
      case OKPPublicKey(kty, crv, x, kid) =>
        Try(
          BSONDocument(
            "kty" -> kty,
            "crv" -> crv,
            "x" -> x,
            "kid" -> kid,
          )
        )
    }
}
given BSONDocumentReader[PublicKey] with {

  override def readDocument(doc: BSONDocument): Try[PublicKey] =
    doc.get("kty").get.asTry[KTY] match
      case Failure(exception) => Failure(exception)
      case Success(KTY.OKP) =>
        (
          doc.getAsTry[Curve]("crv"),
          doc.getAsTry[String]("x"),
        ) match
          case (Success(crv), Success(x)) =>
            Success(
              OKPPublicKey(
                kty = KTY.OKP,
                crv = crv,
                x = x,
                kid = doc.getAsOpt[String]("kid")
              )
            )
          case (Failure(ex), _) => Failure(ex)
          case (_, Failure(ex)) => Failure(ex)
      case Success(KTY.EC) =>
        (
          doc.getAsTry[Curve]("crv"),
          doc.getAsTry[String]("x"),
          doc.getAsTry[String]("y"),
        ) match
          case (Success(crv), Success(x), Success(y)) =>
            Success(
              ECPublicKey(
                kty = KTY.EC,
                crv = crv,
                x = x,
                y = y,
                kid = doc.getAsOpt[String]("kid")
              )
            )
          case (Failure(ex), _, _) => Failure(ex)
          case (_, Failure(ex), _) => Failure(ex)
          case (_, _, Failure(ex)) => Failure(ex)

}

given BSONDocumentWriter[AnonProtectedHeader] =
  Macros.writer[AnonProtectedHeader]
given BSONDocumentReader[AnonProtectedHeader] =
  Macros.reader[AnonProtectedHeader]

given BSONDocumentWriter[AuthProtectedHeader] =
  Macros.writer[AuthProtectedHeader]
given BSONDocumentReader[AuthProtectedHeader] =
  Macros.reader[AuthProtectedHeader]

given BSONDocumentWriter[ProtectedHeader] =
  Macros.writer[ProtectedHeader]
given BSONDocumentReader[ProtectedHeader] =
  Macros.reader[ProtectedHeader]

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
