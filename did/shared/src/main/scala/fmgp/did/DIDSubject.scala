package fmgp.did

import zio.json._

/** The entity identified by a DID and described by a DID document. Anything can be a DID subject: person, group,
  * organization, physical thing, digital thing, logical thing, etc.
  */
opaque type DIDSubject = String
object DIDSubject {

  extension (x: DIDSubject)
    def value: String = x
    def toDID: DID = new {
      val (namespace, specificId) = DID.getNamespaceAndSpecificId(x)
    }
  given Conversion[DIDSubject, DID] = _.toDID

  def apply(s: String): DIDSubject = s

  implicit val decoder: JsonDecoder[DIDSubject] = JsonDecoder.string.map(s => DIDSubject(s))
  implicit val encoder: JsonEncoder[DIDSubject] = JsonEncoder.string.contramap(e => e.value)

}
