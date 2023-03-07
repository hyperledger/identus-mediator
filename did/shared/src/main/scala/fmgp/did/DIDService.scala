package fmgp.did

import zio.json._
import scala.util.chaining._

/** DIDService
  *
  * https://w3c.github.io/did-core/#service-properties
  *
  * https://w3c.github.io/did-core/#services
  *
  * @param `type`
  *   https://www.w3.org/TR/did-spec-registries/#service-types
  */
trait DIDService {
  def id: Required[URI]
  def `type`: Required[SetU[String]]
  def serviceEndpoint: Required[SetMapU[URI]]
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
  def getNodes = serviceEndpoint match
    case str: URI                         => Seq.empty
    case seq: Seq[URI] @unchecked         => Seq.empty
    case map: Map[String, URI] @unchecked => map.get("nodes")
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

  // extra
  def getServiceEndpointAsURIs: Seq[URI] = serviceEndpoint match
    case str: URI                         => Seq(str)
    case seq: Seq[URI] @unchecked         => seq
    case map: Map[String, URI] @unchecked => map.values.toSeq

  def getServiceEndpointNextForward = getServiceEndpointAsURIs.flatMap(uri =>
    uri match {
      case s"did:$rest" =>
        fmgp.did.comm.FROMTO.either(uri) match
          case Left(_)       => None // uri
          case Right(fromto) => Some(fromto)
      case other => None // other
    }
  )
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
}

final case class DIDServiceGeneric(
    id: Required[URI],
    `type`: Required[SetU[String]],
    serviceEndpoint: Required[SetMapU[URI]], // FIXME or MAP ???

    // extra for did
    routingKeys: NotRequired[Set[String]] = None,
    accept: NotRequired[Set[String]] = None,
) extends DIDService
    with DIDServiceDIDCommMessaging
    with DIDServiceDIDLinkedDomains
    with DIDServiceDecentralizedWebNode
object DIDServiceClass {
  import SetU.{given}
  import SetMapU.{given}
  implicit val decoder: JsonDecoder[DIDServiceGeneric] =
    DeriveJsonDecoder.gen[DIDServiceGeneric]
  implicit val encoder: JsonEncoder[DIDServiceGeneric] =
    DeriveJsonEncoder.gen[DIDServiceGeneric]
}
