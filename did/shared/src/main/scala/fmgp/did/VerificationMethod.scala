package fmgp.did

import zio.json._

/** RFC7517 - https://www.rfc-editor.org/rfc/rfc7517 */
type JSONWebKeyMap = Map[String, String]

/** MULTIBASE encoded public key.- https://datatracker.ietf.org/doc/html/draft-multiformats-multibase-03 */
type MULTIBASE = String // TODO

/** VerificationMethod
  *
  * https://w3c-ccg.github.io/security-vocab/#verificationMethod
  * https://w3c.github.io/did-core/#verification-method-properties
  */
sealed trait VerificationMethod

object VerificationMethod {
  given decoder: JsonDecoder[VerificationMethod] =
    VerificationMethodClass.decoder.map(e => e).orElse(VerificationMethodReferenced.decoder.map(e => e))
  given encoder: JsonEncoder[VerificationMethod] = {
    VerificationMethodReferenced.encoder
      .orElseEither(VerificationMethodEmbedded.encoder)
      .contramap {
        case l: VerificationMethodReferenced => Left(l)
        case r: VerificationMethodEmbedded   => Right(r)
      }
  }
}

case class VerificationMethodReferenced(value: String) extends VerificationMethod
object VerificationMethodReferenced {
  implicit val decoder: JsonDecoder[VerificationMethodReferenced] =
    JsonDecoder.string.map(e => VerificationMethodReferenced(e))
  implicit val encoder: JsonEncoder[VerificationMethodReferenced] =
    JsonEncoder.string.contramap(e => e.value)
}

trait VerificationMethodEmbedded extends VerificationMethod {
  def id: Required[DIDURLSyntax]
  def controller: Required[DIDSubject]
  def `type`: Required[String]

  def publicKeyJwk: NotRequired[JSONWebKeyMap]

  def publicKeyMultibase: NotRequired[MULTIBASE]
}
object VerificationMethodEmbedded {
  given decoder: JsonDecoder[VerificationMethod] =
    VerificationMethodClass.decoder.map(e => e)
  given encoder: JsonEncoder[VerificationMethodEmbedded] =
    VerificationMethodClass.encoder.contramap(e =>
      VerificationMethodClass(
        id = e.id,
        controller = e.controller,
        `type` = e.`type`,
        publicKeyJwk = e.publicKeyJwk,
        publicKeyMultibase = e.publicKeyMultibase
      )
    )
}

final case class VerificationMethodClass(
    id: Required[DIDURLSyntax], // "did:example:123456789abcdefghi#keys-1",
    controller: Required[DIDSubject], // "did:example:123456789abcdefghi",
    `type`: Required[String], // "Ed25519VerificationKey2020",
    publicKeyJwk: NotRequired[JSONWebKeyMap] = None,
    publicKeyMultibase: NotRequired[MULTIBASE] = None // "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV"
) extends VerificationMethodEmbedded

object VerificationMethodClass {
  implicit val decoder: JsonDecoder[VerificationMethodClass] = DeriveJsonDecoder.gen[VerificationMethodClass]
  implicit val encoder: JsonEncoder[VerificationMethodClass] = DeriveJsonEncoder.gen[VerificationMethodClass]
}
