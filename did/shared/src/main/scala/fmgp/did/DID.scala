package fmgp.did

import zio.json._

//https://github.com/jwtk/jjwt
//https://github.com/panva/jose

//JSON-LD
//https://www.w3.org/ns/did/v1

type Required[A] = A
type NotRequired[A] = Option[A]
type SetU[A] = A | Seq[A]
type Authentication = Option[Set[VerificationMethod]]
object SetU {
  given decoder[U](using jsonDecoder: JsonDecoder[U]): JsonDecoder[U | Seq[U]] =
    jsonDecoder
      .map(e => e: U | Seq[U])
      .orElse(JsonDecoder.seq[U].map(e => e: U | Seq[U]))

  inline given encoder[U](using jsonEncoder: JsonEncoder[U]): JsonEncoder[U | Seq[U]] =
    JsonEncoder.seq[U].contramap { (uuu: (U | Seq[U])) =>
      uuu match {
        case one: U                 => Seq(one)
        case seq: Seq[U] @unchecked => seq
      }
    }
}

trait DID {
  def scheme: String = "did"
  def namespace: String // methodName
  def specificId: String
}
object DID {
  def getNamespaceAndSpecificId(id: DIDSyntax) = id.split(":", 3) match {
    case Array("did", ns, sId) => (ns, sId)
    case _                     => throw new java.lang.AssertionError(s"Fail to parse id: '$id'")
  }
}
type DIDSyntax = String //FIXME
type DIDURLSyntax = String //FIXME
/** RFC3986 - https://www.rfc-editor.org/rfc/rfc3986 */
type URI = String

trait JSONLD {
  def `@context`: String | Seq[String] // = "https://w3id.org/did/v1" // JSON-LD object
}

case class SingleJSONLD(`@context`: String) extends JSONLD
