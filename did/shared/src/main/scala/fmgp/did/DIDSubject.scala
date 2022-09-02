package fmgp.did

import zio.json._
import scala.util.chaining._

/** The entity identified by a DID and described by a DID document. Anything can be a DID subject: person, group,
  * organization, physical thing, digital thing, logical thing, etc.
  */
opaque type DIDSubject = String
object DIDSubject {

  val pattern = """^did:([^\s:]+):([^\s]+)$""".r
  def getNamespaceAndSpecificId(id: String) = id match {
    case pattern(namespace, specificId) => (namespace, specificId)
    case _                              => throw new java.lang.AssertionError(s"Fail to parse id: '$id'")
  }

  extension (id: DIDSubject)
    def value: String = id
    def toDID: DID = new {
      val (namespace, specificId) = getNamespaceAndSpecificId(id) // FIXME unsafe
    }
  given Conversion[DIDSubject, DID] = _.toDID

  def apply(s: String): DIDSubject = s // FIXME perse with REGEX

  implicit val decoder: JsonDecoder[DIDSubject] = JsonDecoder.string.map(s => DIDSubject(s))
  implicit val encoder: JsonEncoder[DIDSubject] = JsonEncoder.string.contramap(e => e.value)

}
