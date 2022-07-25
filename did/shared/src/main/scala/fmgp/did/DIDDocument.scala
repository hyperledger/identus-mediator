package fmgp.did

import zio.json._

type ServiceEndpoint = Set[URI]

/** DIDDocument
  *
  * https://w3c.github.io/did-core/#did-document-properties
  *
  * <https://github.com/w3c/?q=did&type=all&language=&sort>=
  *
  *   - authentication -> challenge-response protocol
  *   - assertionMethod -> Issuer key (for purposes of issuing a Verifiable Credential)
  *   - keyAgreement -> tablishing a secure communication channel with the recipient
  *   - capabilityInvocation -> Master key (for authorization to update the DID Document.)
  *   - capabilityDelegation -> ...
  */
trait DIDDocument extends DID {
  def id: Required[DIDSubject] // = s"$scheme:$namespace:$specificId"
  def alsoKnownAs: NotRequired[Set[String]]
  def controller: NotRequired[Either[String, Set[String]]]
  def verificationMethod: NotRequired[Set[VerificationMethod]]

  def authentication: NotRequired[SetU[VerificationMethod]]
  def assertionMethod: NotRequired[SetU[VerificationMethod]]
  def keyAgreement: NotRequired[Set[VerificationMethod]]
  def capabilityInvocation: NotRequired[SetU[VerificationMethod]]
  def capabilityDelegation: NotRequired[SetU[VerificationMethod]]

  def service: NotRequired[Set[DIDService]] // NotRequired[ServiceEndpoint]

  // methods
  def didSubject = id.toDID

  val (namespace, specificId) = (id.namespace, id.specificId) // DID.getNamespaceAndSpecificId(id)

  def getAuthentications = authentication.toSeq.flatMap {
    case v: VerificationMethod                   => Seq(v)
    case seq: Seq[VerificationMethod] @unchecked => seq
  }
}

object DIDDocument {
  given decoder: JsonDecoder[DIDDocument] =
    DIDDocumentClass.decoder.map(e => e)
  given encoder: JsonEncoder[DIDDocument] =
    DIDDocumentClass.encoder.contramap(e =>
      DIDDocumentClass(
        id = e.id,
        alsoKnownAs = e.alsoKnownAs,
        controller = e.controller,
        verificationMethod = e.verificationMethod,
        authentication = e.authentication,
        assertionMethod = e.assertionMethod,
        keyAgreement = e.keyAgreement,
        capabilityInvocation = e.capabilityInvocation,
        capabilityDelegation = e.capabilityDelegation,
        service = e.service,
      )
    )
}

case class DIDDocumentClass(
    id: Required[DIDSubject],
    alsoKnownAs: NotRequired[Set[String]] = None,
    controller: NotRequired[Either[String, Set[String]]] = None,
    verificationMethod: NotRequired[Set[VerificationMethod]] = None,
    authentication: NotRequired[SetU[VerificationMethod]] = None,
    assertionMethod: NotRequired[SetU[VerificationMethod]] = None,
    keyAgreement: NotRequired[Set[VerificationMethod]] = None,
    capabilityInvocation: NotRequired[SetU[VerificationMethod]] = None,
    capabilityDelegation: NotRequired[SetU[VerificationMethod]] = None,
    service: NotRequired[Set[DIDService]] = None,
) extends DIDDocument

object DIDDocumentClass {
  import SetU.{given}
  implicit val decoder: JsonDecoder[DIDDocumentClass] = DeriveJsonDecoder.gen[DIDDocumentClass]
  implicit val encoder: JsonEncoder[DIDDocumentClass] = DeriveJsonEncoder.gen[DIDDocumentClass]
}
