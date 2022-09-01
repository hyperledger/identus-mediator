package fmgp.did.comm

import zio.json._
import fmgp.did._
import fmgp.crypto.OKP_EC_Key
import zio.json.ast.Json
import zio.json.ast.JsonCursor

// case class AnoncryptMessages //TODO
// case class AuthcryptMessages //TODO

/** TODO - To prevent invalid curve and weak point attacks, implementations that decrypt messages from a NIST curve MUST
  * verify that the received public key (contained in the JWE protected header) is on the curve in question. This check
  * may already be done by some JOSE libraries, but developers should not assume this is the case. See this explanation
  * of the risk, and this practical guide for how to perform the verification correctly.
  * https://neilmadden.blog/2017/05/17/so-how-do-you-validate-nist-ecdh-public-keys/
  */
case class EncryptedMessageGeneric(
    ciphertext: Base64URL,
    `protected`: Base64URLHeaders,
    recipients: Seq[Recipient],
    tag: AuthenticationTag,
    iv: InitializationVector
) extends EncryptedMessage

object EncryptedMessageGeneric {
  given decoder: JsonDecoder[EncryptedMessageGeneric] = DeriveJsonDecoder.gen[EncryptedMessageGeneric]
  given encoder: JsonEncoder[EncryptedMessageGeneric] = DeriveJsonEncoder.gen[EncryptedMessageGeneric]
}
