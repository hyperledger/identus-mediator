package fmgp.did

import zio.json._
import scala.util.chaining._
import zio.json.ast.Json
import zio.json.ast.Json.Obj
import zio.json.ast.Json.Arr
import zio.json.ast.Json.Bool
import zio.json.ast.Json.Str
import zio.json.ast.Json.Num
import zio.json.ast.JsonCursor

/** DIDService
  *
  * https://w3c.github.io/did-core/#service-properties
  *
  * https://w3c.github.io/did-core/#services
  *
  * @param `type`
  *   https://www.w3.org/TR/did-spec-registries/#service-types
  * @param serviceeEndpoint
  *   A string that conforms to the rules of [RFC3986] for URIs, a map, or a set composed of a one or more strings that
  *   conform to the rules of [RFC3986] for URIs and/or maps.
  */
trait DIDService {
  def id: Required[URI]
  def `type`: Required[SetU[String]]
  def serviceEndpoint: Required[ServiceEndpoint]
}

/** DecentralizedWebNode is a type of DIDService
  *
  * @see
  *   https://identity.foundation/decentralized-web-node/spec/#service-endpoints
  *
  * {{{
  * "serviceEndpoint": {"nodes": ["https://dwn.example.com", "https://example.org/dwn"]}
  * }}}
  */
trait DIDServiceDecentralizedWebNode extends DIDService {
  // override def `type` = "DecentralizedWebNode"

  def getNodes: Seq[String] = serviceEndpoint match
    case Json.Str(str) => Seq.empty
    case Json.Arr(elements) =>
      elements.toSeq.flatMap {
        case obj: Json.Obj =>
          obj.get(JsonCursor.field("nodes")) match
            case Left(_)                   => Seq.empty
            case Right(Json.Arr(elements)) => elements.collect { case Str(v) => v }
            case Right(_)                  => Seq.empty
        case _ => None
      }
    case obj: Json.Obj =>
      obj.get(JsonCursor.field("nodes")) match
        case Left(_)                   => Seq.empty
        case Right(Json.Arr(elements)) => elements.collect { case Str(v) => v }
        case Right(_)                  => Seq.empty

  // serviceEndpoint match
  //  case str: URI                              => Seq.empty
  //  case seq: Seq[URI] @unchecked              => Seq.empty
  //  case map: Map[String, URI] @unchecked      => map.get("nodes").toSeq
  //  case seq: Seq[Map[String, URI]] @unchecked => seq.flatMap(_.get("nodes"))
}

/** https://www.w3.org/TR/did-spec-registries/#linkeddomains */
trait DIDServiceDIDLinkedDomains extends DIDService {
  // override def `type` = "LinkedDomains"
  // TODO FIX "serviceEndpoint": {"origins": ["https://foo.example.com", "https://identity.foundation"]}
}

/** https://www.w3.org/TR/did-spec-registries/#didcommmessaging */
trait DIDServiceDIDCommMessaging extends DIDService {
  // override def `type` = "DIDCommMessaging"
  def routingKeys: NotRequired[Set[String]]
  def accept: NotRequired[Set[String]]
}

object DIDService {
  val TYPE_DecentralizedWebNode = "DecentralizedWebNode"
  val TYPE_DIDCommMessaging = "DIDCommMessaging"
  val TYPE_LinkedDomains = "LinkedDomains"

  given decoder: JsonDecoder[DIDService] =
    DIDServiceClass.decoder.map(e => e)
  given encoder: JsonEncoder[DIDService] =
    DIDServiceClass.encoder.contramap(_ match {
      case e: DIDServiceGeneric => e
      case other: DIDService =>
        DIDServiceGeneric(
          id = other.id,
          `type` = other.`type`,
          serviceEndpoint = other.serviceEndpoint,
        )
    })

  extension (service: DIDService)
    def getServiceEndpointAsURIs: Seq[URI] = service.serviceEndpoint match
      case Json.Str(str) => Seq(str)
      case seq: Json.Arr =>
        seq.elements.toSeq.flatMap {
          case Str(value)  => Some(value)
          case Obj(fields) => None // this will change on DID Comm 2.1
          case _           => None
        }
      case Json.Obj(fields) =>
        fields
          .filter(_._1 == "uri")
          .map(_._2)
          .flatMap {
            case Str(uri) => Some(uri)
            case _        => None
          }
          .toSeq

    def getServiceEndpointNextForward = service.getServiceEndpointAsURIs.flatMap(uri =>
      uri match {
        case s"did:$rest" =>
          fmgp.did.comm.FROMTO.either(uri) match
            case Left(_)       => None // uri
            case Right(fromto) => Some(fromto)
        case other => None // other
      }
    )
}

final case class DIDServiceGeneric(
    id: Required[URI],
    `type`: Required[SetU[String]],
    serviceEndpoint: Required[ServiceEndpoint],

    // extra for did
    routingKeys: NotRequired[Set[String]] = None,
    accept: NotRequired[Set[String]] = None,
) extends DIDService
    with DIDServiceDIDCommMessaging
    with DIDServiceDIDLinkedDomains
    with DIDServiceDecentralizedWebNode
object DIDServiceClass {
  import SetU.{given}
  import ServiceEndpoint.{given}
  implicit val decoder: JsonDecoder[DIDServiceGeneric] =
    DeriveJsonDecoder.gen[DIDServiceGeneric]
  implicit val encoder: JsonEncoder[DIDServiceGeneric] =
    DeriveJsonEncoder.gen[DIDServiceGeneric]
}
