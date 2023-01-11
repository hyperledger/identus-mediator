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
object DIDServiceClass {
  import SetU.{given}
  import SetMapU.{given}
  implicit val decoder: JsonDecoder[DIDServiceGeneric] =
    DeriveJsonDecoder.gen[DIDServiceGeneric]
  implicit val encoder: JsonEncoder[DIDServiceGeneric] =
    DeriveJsonEncoder.gen[DIDServiceGeneric]
}
