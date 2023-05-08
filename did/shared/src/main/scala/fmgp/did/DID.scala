package fmgp.did

import zio.json._
import zio.json.ast.Json
import fmgp.did.comm.{FROM, FROMTO, TO}

//https://github.com/jwtk/jjwt
//https://github.com/panva/jose

//JSON-LD
//https://www.w3.org/ns/did/v1

type Required[A] = A
type NotRequired[A] = Option[A]
type SetU[A] = A | Seq[A]
// type SetMapU[A] = A | Seq[A] | Map[String, A]
// type ServiceEndpoint = URI | Map[String, URI] | Seq[URI] | Seq[Map[String, URI]] //SetU[URI]
type ServiceEndpoint = Json.Str | Json.Arr | Json.Obj
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

object ServiceEndpoint {
  given decoder: JsonDecoder[ServiceEndpoint] =
    summon[JsonDecoder[Json]].mapOrFail {
      case j: Json.Null => Left("ServiceEndpoint can not be 'null'")
      case j: Json.Bool => Left("ServiceEndpoint can not be Boolean")
      case j: Json.Num  => Left("ServiceEndpoint can not be Numbre")
      case j: Json.Arr =>
        j match
          case e if e.elements.toVector.forall(_.isInstanceOf[Json.Str]) => Right(j)
          case e if e.elements.toVector.forall(_.isInstanceOf[Json.Obj]) => Right(j)
          case e => Left("ServiceEndpoint can be Array can olny be of Strings of Objects")
      case j: Json.Str => Right(j)
      case j: Json.Obj => Right(j)
    }

  given encoder: JsonEncoder[ServiceEndpoint] =
    summon[JsonEncoder[Json]].contramap(e => e)
}

trait DID {
  def scheme: String = "did"
  def namespace: String // methodName
  def specificId: String

  /** This is the full identifier */
  def string = s"$scheme:$namespace:$specificId"
  def did = string

  // override def toString(): String = string
}
object DID {
  given Conversion[DID, DIDSubject] = did => DIDSubject(did.scheme + ":" + did.namespace + ":" + did.specificId)
  given Conversion[DID, TO] = did => TO.unsafe_apply(did.scheme + ":" + did.namespace + ":" + did.specificId)
  given Conversion[DID, FROM] = did => FROM.unsafe_apply(did.scheme + ":" + did.namespace + ":" + did.specificId)
  given Conversion[DID, FROMTO] = did => FROMTO.unsafe_apply(did.scheme + ":" + did.namespace + ":" + did.specificId)
}

type DIDSyntax = String //FIXME
type DIDURLSyntax = String //FIXME
type DIDController = DIDSyntax //FIXME
/** RFC3986 - https://www.rfc-editor.org/rfc/rfc3986 */
type URI = String

trait JSONLD {
  def `@context`: String | Seq[String] // = "https://w3id.org/did/v1" // JSON-LD object
}

case class SingleJSONLD(`@context`: String) extends JSONLD
