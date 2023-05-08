package fmgp.did.comm

import util.chaining.scalaUtilChainingOps
import zio.json._

import fmgp.util.Base64

/** OutOfBand make more type safe for OOB with SignedMessage or PlaintextMessage */

sealed trait OutOfBand {
  def msg: Message
  def data: Base64
  def makeURI(base: String) = base + "?_oob=" + data.urlBase64
}
case class OutOfBandPlaintext private (msg: PlaintextMessage, data: Base64) extends OutOfBand
object OutOfBandPlaintext {
  def from(base64: Base64): Either[String, OutOfBandPlaintext] =
    base64.decodeToString.fromJson[PlaintextMessage].map(msg => OutOfBandPlaintext(msg, base64))
  def from(msg: PlaintextMessage) =
    OutOfBandPlaintext(msg, Base64.encode(msg.toJson))
}
case class OutOfBandSigned private (msg: SignedMessage, data: Base64) extends OutOfBand
object OutOfBandSigned {
  def from(base64: Base64): Either[String, OutOfBandSigned] =
    base64.decodeToString.fromJson[SignedMessage].map(msg => OutOfBandSigned(msg, base64))
  def from(msg: SignedMessage) =
    OutOfBandSigned(msg, Base64.encode(msg.toJson))
}

/** OOB - OutOfBand */
object OutOfBand {
  private val errorInfo = Left("Missing '_oob'")

  def safeBase64(oobBase64: String): Either[String, OutOfBand] =
    Base64
      .safeBase64url(oobBase64)
      .left
      .map(error => s"'_oob' $error")
      .flatMap(data => OutOfBandSigned.from(data).orElse(OutOfBandPlaintext.from(data)))

  def oob(str: String): Either[String, OutOfBand] =
    oobSigned(str)
      .orElse(oobPlaintext(str))
      .orElse(oob(str).map(Right(_)).getOrElse(errorInfo))

  def oobPlaintext(str: String): Either[String, OutOfBandPlaintext] =
    parse(str) match
      case None         => errorInfo
      case Some(base64) => OutOfBandPlaintext.from(base64)
  def oobSigned(str: String): Either[String, OutOfBandSigned] =
    parse(str) match
      case None         => errorInfo
      case Some(base64) => OutOfBandSigned.from(base64)

  def parse(str: String): Option[Base64] = parseURI(str)
  private val pattern = """^[^\?\#\s]*\?[^\#\s:]*_oob=([^\#&\s:]+)[^\#\s:]*(\#[^\s]*)?$""".r
  private inline def parseURI(id: String) = id match {
    case pattern(oob, fragment) => Option(oob).filterNot(_.isEmpty()).map(Base64.fromBase64url(_))
    case _                      => None
  }
}
