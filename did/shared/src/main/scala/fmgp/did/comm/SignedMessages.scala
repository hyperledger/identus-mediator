package fmgp.did.comm

import zio.json._

/** SignedMessage is a JSON Web Message (JWM)
  *
  * JWM is a flexible way to encode application-level messages in JSON for transfer over a variety of transport
  * protocols. JWMs use JSON Web Encryption (JWE) to protect integrity, achieve confidentiality, and achieve repudiable
  * authentication; alternatively or in addition, they use JSON Web Signatures (JWS) to associate messages with a
  * non-repudiable digital signature.
  *
  * https://datatracker.ietf.org/doc/html/draft-looker-jwm-01
  */
case class SignedMessage(
    payload: Base64URL,
    signatures: Seq[JWMSignatureObj]
) {
  def base64 = signatures.head.`protected` + "." + payload + "." + signatures.head.signature
}

object SignedMessage {
  given decoder: JsonDecoder[SignedMessage] = DeriveJsonDecoder.gen[SignedMessage]
  given encoder: JsonEncoder[SignedMessage] = DeriveJsonEncoder.gen[SignedMessage]
}

case class JWMSignatureObj(`protected`: String, signature: String, header: Option[JWMHeader] = None)
object JWMSignatureObj {
  given decoder: JsonDecoder[JWMSignatureObj] = DeriveJsonDecoder.gen[JWMSignatureObj]
  given encoder: JsonEncoder[JWMSignatureObj] = DeriveJsonEncoder.gen[JWMSignatureObj]
}

case class JWMHeader(kid: String)
object JWMHeader {
  given decoder: JsonDecoder[JWMHeader] = DeriveJsonDecoder.gen[JWMHeader]
  given encoder: JsonEncoder[JWMHeader] = DeriveJsonEncoder.gen[JWMHeader]
}
