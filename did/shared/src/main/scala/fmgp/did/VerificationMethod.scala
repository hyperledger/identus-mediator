package fmgp.did

import zio.json._
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import fmgp.crypto.PublicKey
import fmgp.crypto.OKPPublicKey
import fmgp.did.comm.FROMTO

/** MULTIBASE encoded public key.- https://datatracker.ietf.org/doc/html/draft-multiformats-multibase-03 */
type MULTIBASE = String // TODO

/** VerificationMethod
  *
  * https://w3c-ccg.github.io/security-vocab/#verificationMethod
  * https://w3c.github.io/did-core/#verification-method-properties
  * https://www.w3.org/TR/did-core/#verification-method-properties
  */
sealed trait VerificationMethod {
  def id: String // DID URL Syntax.
//   def controller: String // DID Syntax.
//   def `type`: String
//   def publicKeyJwk: String // RFC7517
//   def publicKeyMultibase: String // MULTIBASE
}

object VerificationMethod {
  given decoder: JsonDecoder[VerificationMethod] =
    VerificationMethodEmbedded.decoder.map(e => e).orElse(VerificationMethodReferenced.decoder.map(e => e))
  given encoder: JsonEncoder[VerificationMethod] = {
    VerificationMethodReferenced.encoder
      .orElseEither(VerificationMethodEmbedded.encoder)
      .contramap {
        case l: VerificationMethodReferenced => Left(l)
        case r: VerificationMethodEmbedded   => Right(r)
      }
  }
}

case class VerificationMethodReferencedWithKey[K <: fmgp.crypto.OKP_EC_Key](kid: String, key: K) {
  def vmr = VerificationMethodReferenced(kid)
  def pair = (vmr, key) // TODO REMOVE
}
object VerificationMethodReferencedWithKey {
  def from[K <: fmgp.crypto.OKP_EC_Key](vmr: VerificationMethodReferenced, key: K) =
    VerificationMethodReferencedWithKey(vmr.value, key)
}

case class VerificationMethodReferenced(value: String) extends VerificationMethod {
  def did = DIDSubject(value.split('#').head)
  def fromto = FROMTO(value.split('#').head) // FIXME
  def id = value // TODO rename value to id
}
object VerificationMethodReferenced {
  given decoder: JsonDecoder[VerificationMethodReferenced] = JsonDecoder.string.map(VerificationMethodReferenced.apply)
  given encoder: JsonEncoder[VerificationMethodReferenced] = JsonEncoder.string.contramap(_.value)

  // These given are useful if we use the VerificationMethodReferenced as a Key (ex: Map[VerificationMethodReferenced , Value])
  given JsonFieldDecoder[VerificationMethodReferenced] = JsonFieldDecoder.string.map(VerificationMethodReferenced.apply)
  given JsonFieldEncoder[VerificationMethodReferenced] = JsonFieldEncoder.string.contramap(_.value)
}

sealed trait VerificationMethodEmbedded extends VerificationMethod {
  def id: Required[DIDURLSyntax]
  def controller: Required[DIDController]
  def `type`: Required[String]

  // def publicKeyJwk: NotRequired[JSONWebKeyMap]
  // def publicKeyMultibase: NotRequired[MULTIBASE]
  /** this is a Either publicKeyJwk or a publicKeyMultibase */
  def publicKey: Either[MULTIBASE, PublicKey]
}

/** VerificationMethodEmbeddedJWK
  *
  * @param publicKeyJwk
  *   is a JSON Web Key (JWK) - RFC7517 - https://www.rfc-editor.org/rfc/rfc7517
  */
case class VerificationMethodEmbeddedJWK(
    id: Required[DIDURLSyntax], // "did:example:123456789abcdefghi#keys-1",
    controller: Required[DIDController], // "did:example:123456789abcdefghi",
    `type`: Required[String], // "Ed25519VerificationKey2020",
    publicKeyJwk: Required[PublicKey]
) extends VerificationMethodEmbedded {
  def publicKey = Right(publicKeyJwk)
}

object VerificationMethodEmbeddedJWK {
  given decoder: JsonDecoder[VerificationMethodEmbeddedJWK] =
    DeriveJsonDecoder.gen[VerificationMethodEmbeddedJWK]
  given encoder: JsonEncoder[VerificationMethodEmbeddedJWK] =
    DeriveJsonEncoder.gen[VerificationMethodEmbeddedJWK]
}

/** VerificationMethodEmbeddedMultibase
  */
case class VerificationMethodEmbeddedMultibase(
    id: Required[DIDURLSyntax], // "did:example:123456789abcdefghi#keys-1",
    controller: Required[DIDController], // "did:example:123456789abcdefghi",
    `type`: Required[String], // "Ed25519VerificationKey2020",
    publicKeyMultibase: Required[MULTIBASE]
) extends VerificationMethodEmbedded {
  def publicKey = Left(publicKeyMultibase)
}

object VerificationMethodEmbeddedMultibase {
  given decoder: JsonDecoder[VerificationMethodEmbeddedMultibase] =
    DeriveJsonDecoder.gen[VerificationMethodEmbeddedMultibase]
  given encoder: JsonEncoder[VerificationMethodEmbeddedMultibase] =
    DeriveJsonEncoder.gen[VerificationMethodEmbeddedMultibase]
}

object VerificationMethodEmbedded {
  given decoder: JsonDecoder[VerificationMethod] =
    Json.Obj.decoder.mapOrFail { originalAst =>
      if (originalAst.fields.exists(e => e._1 == "publicKeyJwk"))
        VerificationMethodEmbeddedJWK.decoder.decodeJson(originalAst.toJson)
      else // publicKeyMultibase
        VerificationMethodEmbeddedMultibase.decoder.decodeJson(originalAst.toJson)
    }

  given encoder: JsonEncoder[VerificationMethodEmbedded] = new JsonEncoder[VerificationMethodEmbedded] {
    override def unsafeEncode(
        b: VerificationMethodEmbedded,
        indent: Option[Int],
        out: zio.json.internal.Write
    ): Unit = b match {
      case obj: VerificationMethodEmbeddedJWK =>
        VerificationMethodEmbeddedJWK.encoder.unsafeEncode(obj, indent, out)
      case obj: VerificationMethodEmbeddedMultibase =>
        VerificationMethodEmbeddedMultibase.encoder.unsafeEncode(obj, indent, out)
    }
  }
}
