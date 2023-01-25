package fmgp.did.comm

import zio.json._

// import java.net.URI // no scalajs
// import java.net.URL // no scalajs
import fmgp.util.Base64

import util.chaining.scalaUtilChainingOps

/** OutOfBand make more type safe for OOB with SignedMessage or PlaintextMessage */

sealed trait OutOfBand {
  def data: Base64
}
case class OOB(data: Base64) extends OutOfBand
case class OutOfBandPlaintext private (msg: PlaintextMessage, val data: Base64) extends OutOfBand {}
object OutOfBandPlaintext {
  def from(base64: Base64): Either[String, OutOfBandPlaintext] =
    base64.decodeToString.fromJson[PlaintextMessage].map(msg => OutOfBandPlaintext(msg, base64))
}
case class OutOfBandSigned private (msg: SignedMessage, val data: Base64) extends OutOfBand
object OutOfBandSigned {
  def from(base64: Base64): Either[String, OutOfBandSigned] =
    base64.decodeToString.fromJson[SignedMessage].map(msg => OutOfBandSigned(msg, base64))
}

/** OOB
  */
object OutOfBand {
  private val errorInfo = Left("Missing '_oob'")

  def from(str: String): Either[String, OutOfBand] =
    oobSigned(str)
      .orElse(oobPlaintext(str))
      .orElse(oob(str).map(Right(_)).getOrElse(errorInfo))

  def oobPlaintext(str: String): Either[String, OutOfBandPlaintext] =
    readData(str) match
      case None         => errorInfo
      case Some(base64) => OutOfBandPlaintext.from(base64)
  def oobSigned(str: String): Either[String, OutOfBandSigned] =
    readData(str) match
      case None         => errorInfo
      case Some(base64) => OutOfBandSigned.from(base64)

  // def oob(uri: URI): Option[OOB] = readData(uri).map(OOB(_))
  // def oob(url: URL): Option[OOB] = readData(url).map(OOB(_))
  def oob(str: String): Option[OOB] = readData(str).map(OOB(_))

  // def readData(uri: URI): Option[Base64] = parseURI("?" + uri.getQuery)
  // def readData(url: URL): Option[Base64] = parseURI("?" + url.getQuery)
  def readData(str: String): Option[Base64] = parseURI(str)

  private val pattern = """^[^\?\#\s]*\?[^\#\s:]*_oob=([^\#&\s:]+)[^\#\s:]*(\#[^\s]*)?$""".r
  private inline def parseURI(id: String) = id match {
    case pattern(oob, fragment) => Option(oob).filterNot(_.isEmpty()).map(Base64.fromBase64url(_))
    case _                      => None
  }
}
