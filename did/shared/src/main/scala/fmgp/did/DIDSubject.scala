package fmgp.did

import zio.json._
import scala.util.chaining._
import fmgp.crypto.error._

/** The entity identified by a DID and described by a DID document. Anything can be a DID subject: person, group,
  * organization, physical thing, digital thing, logical thing, etc.
  *
  * NOTE URL_Path is path of the DIDSubject!
  *
  * TODO deprecated in favor of FROMTO
  */
opaque type DIDSubject = String
object DIDSubject {

  val pattern = """^did:([^\s:]+):([^\?\#\s]+)(?!\?[^\#\s:]*)(?!\#.*)$""".r // OLD """^did:([^\s:]+):([^\s]+)$""".r

  /** @throws AssertionError if not a valid DIDSubject */
  inline def parseString(id: String) = id match {
    case pattern(namespace, specificId) => (namespace, specificId)
    case _                              => throw new java.lang.AssertionError(s"Fail to parse DIDSubject: '$id'")
  }

  extension (id: DIDSubject)
    private inline def value: String = id
    // inline def inline_namespace: String = parseString(id)._1
    // inline def inline_specificId: String = parseString(id)._2
    inline def subject: String = parseString(id)._2
    // inline def asTO = fmgp.did.comm.TO.unsafe_apply(id)
    // inline def asFROM = fmgp.did.comm.FROM.unsafe_apply(id)
    // inline def asFROMTO = fmgp.did.comm.FROMTO.unsafe_apply(id)

    def toDID: DID = new {
      val (namespace, specificId) = parseString(id) // FIXME unsafe
    }

    def sameSubject(that: DIDSubject): Boolean =
      toDID.namespace == that.namespace & subject == that.subject
    def sameSubjectQ(that: DIDSubjectQ): Boolean =
      id.toDID.namespace == fmgp.did.DIDSubject$package.DIDSubjectQ.inline_namespace(that) // inline
        & id.subject == fmgp.did.DIDSubject$package.DIDSubjectQ.subject(that) // inline

  given Conversion[DIDSubject, DID] = _.toDID
  // This is an one way Conversion
  given Conversion[DIDSubject, DIDSubjectQ] = e => DIDSubjectQ(e)
  // given Conversion[DIDSubject, DIDURL] = e => DIDURL(e)

  /** unsave TODO rename to maybe instead of apply
    *
    * @throws AssertionError
    *   if not a valid DIDSubject
    */
  def apply(s: String): DIDSubject = s.tap(e => parseString(e)) // 'tap' is to throws as soon as possible
  def either(s: String): Either[FailToParse, DIDSubject] =
    if (pattern.matches(s)) Right(DIDSubject(s)) else Left(FailToParse(s"NOT a DID subject '$s'"))

  given decoder: JsonDecoder[DIDSubject] = JsonDecoder.string.map(s => DIDSubject(s)) // TODO use either
  given encoder: JsonEncoder[DIDSubject] = JsonEncoder.string.contramap(e => e.value)
  // These given are useful if we use the DIDSubject as a Key (ex: Map[DIDSubject , Value])
  given JsonFieldDecoder[DIDSubject] = JsonFieldDecoder.string.map(s => DIDSubject(s)) // TODO use either
  given JsonFieldEncoder[DIDSubject] = JsonFieldEncoder.string.contramap(e => e.value)

}

/** TODO rename to DIDSubjectEXTRA or REMOVE
  *
  * DIDSubjectQ is a DIDSubject with Path and Query parameter (that can be missing)
  *
  * NOTE if missing the behavior of using a DIDSubjectQ MUST be the same as using a DIDSubject NOTE that DIDSubject can
  * be implicitly converted into a DIDSubjectQ
  *
  * NOTE URL_Path is path of the DIDSubject!
  *
  * NOTE: override the equals method is F*** hard that to do it right, so left no not shot ourself on the foot!!
  * {{{
  * override def equals(that: Any): Boolean = that match {
  *   case that: DIDSubjectWithQuery =>
  *     that.canEqual(this) && this.subject.hashCode == this.subject.hashCode
  *   case _ => false
  * }
  * }}}
  */
opaque type DIDSubjectQ = String
object DIDSubjectQ {

  /** https://www.regextester.com/15
    * {{{
    * MATCH:
    * did:m:s?q
    * did:m:s
    * did:m:s?
    * did:m:s????
    *
    * DO NOT MATCH:
    * did:m:s?q#f
    * did:m:s#
    * did:m:s#f
    * did:m:s/p?q#f
    * }}}
    */
  val pattern = """^did:([^\s:]+):([^\?\#\s]+)(\?[^\#\s:]*)?(?!\#.*)$""".r

  /** @throws AssertionError if not a valid DIDSubjectQ */
  inline def parseString(id: String) = id match {
    case pattern(namespace, subject, null)  => (namespace, subject, "") // same as a DIDSubject
    case pattern(namespace, subject, query) => (namespace, subject, query.drop(1)) // drop '?' you can on a emply string
    case _                                  => throw new java.lang.AssertionError(s"Fail to parse DIDSubjectQ: '$id'")
  }

  extension (id: DIDSubjectQ)
    private inline def value: String = id
    inline def inline_namespace: String = parseString(id)._1
    inline def subject: String = parseString(id)._2
    inline def query: String = parseString(id)._3
    def toDID: DID = new { // FIXME unsafe
      val namespace = parseString(id)._1
      val specificId = parseString(id).pipe { e =>
        e._2 + (if (!e._3.isEmpty | value.endsWith("?")) "?" + e._3 else "")
      }
    }

    inline def sameSubject(that: DIDSubject): Boolean =
      toDID.namespace == that.namespace & subject == that.subject
    inline def sameSubjectQ(that: DIDSubjectQ): Boolean =
      toDID.namespace == that.namespace & subject == that.subject

  /** This SHOULD NOT BE an implicit conversion
    *
    * FIXME DIDSubjectQ (DID with Query) is should probably not be a DID!
    */
  given Conversion[DIDSubjectQ, DID] = _.toDID
  // given Conversion[DIDSubjectQ, DIDURL] = e => DIDURL(subject = e.subject, query = e.query)

  /** unsave TODO rename to maybe instead of apply
    *
    * @throws AssertionError
    *   if not a valid DIDSubjectQ
    */
  def apply(s: String): DIDSubjectQ = s.tap(e => parseString(e)) // 'tap' is to throws as soon as possible
  def either(s: String): Either[FailToParse, DIDSubjectQ] =
    if (pattern.matches(s)) Right(DIDSubjectQ(s)) else Left(FailToParse(s"NOT a DID subject with query '$s'"))

  given decoder: JsonDecoder[DIDSubjectQ] = JsonDecoder.string.map(s => DIDSubjectQ(s)) // TODO use either
  given encoder: JsonEncoder[DIDSubjectQ] = JsonEncoder.string.contramap(e => e.value)
  // These given are useful if we use the DIDSubject as a Key (ex: Map[DIDSubjectQ , Value])
  given JsonFieldDecoder[DIDSubjectQ] = JsonFieldDecoder.string.map(s => DIDSubjectQ(s)) // TODO use either
  given JsonFieldEncoder[DIDSubjectQ] = JsonFieldEncoder.string.contramap(e => e.value)

}
