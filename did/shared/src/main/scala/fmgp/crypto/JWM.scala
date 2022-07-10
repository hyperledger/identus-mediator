package fmgp.crypto

import zio.json._

case class JWMSignatureObj(`protected`: String, signature: String, header: Option[Seq[JWMHeader]] = None)
object JWMSignatureObj {
  given decoder: JsonDecoder[JWMSignatureObj] = DeriveJsonDecoder.gen[JWMSignatureObj]
  given encoder: JsonEncoder[JWMSignatureObj] = DeriveJsonEncoder.gen[JWMSignatureObj]
}

case class JWMHeader(kid: String)
object JWMHeader {
  given decoder: JsonDecoder[JWMHeader] = DeriveJsonDecoder.gen[JWMHeader]
  given encoder: JsonEncoder[JWMHeader] = DeriveJsonEncoder.gen[JWMHeader]
}

case class JWM(payload: String, signatures: Seq[JWMSignatureObj]) {
  def base64 = signatures.head.`protected` + "." + payload + "." + signatures.head.signature
}

/** JSON Web Message (JWM)
  *
  * JWM is a flexible way to encode application-level messages in JSON for transfer over a variety of transport
  * protocols. JWMs use JSON Web Encryption (JWE) to protect integrity, achieve confidentiality, and achieve repudiable
  * authentication; alternatively or in addition, they use JSON Web Signatures (JWS) to associate messages with a
  * non-repudiable digital signature.
  *
  * https://datatracker.ietf.org/doc/html/draft-looker-jwm-01
  */
object JWM {
  given decoder: JsonDecoder[JWM] = DeriveJsonDecoder.gen[JWM]
  given encoder: JsonEncoder[JWM] = DeriveJsonEncoder.gen[JWM]

}
