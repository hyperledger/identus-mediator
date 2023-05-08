package fmgp.did.comm

import zio.json._
import scala.util.chaining._

import fmgp.did._
import fmgp.crypto.error._

private[comm] object FromTo {

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
    *
    * --------------------^did:|-method|:|-----id-----||----path-----||---query---||no-fragment$
    */
  inline def pattern = """^did:([^\s:]+):([^\/\?\#\s]+)(\/[^\?\#\s]*)?(\?[^\#\s]*)?(?![^\#\s]+)$""".r

  /** @throws AssertionError if not a valid DIDURL with no fragment */
  inline def unsafe_parse(did: String) = did match {
    case pattern(namespace, subject, path, query) =>
      DIDURL(
        namespace,
        subject,
        Option(path).getOrElse(""), // will have the '/' if presente
        Option(query).getOrElse(""), // will have the '?' if presente
      )
    case _ => throw new java.lang.AssertionError(s"MUST be DID URL with no fragment '$did'")
  }

  inline def either_parse(did: String) = did match { // pattern.matches(s)
    case pattern(namespace, subject, path, query) =>
      Right(
        DIDURL(
          namespace,
          subject,
          Option(path).getOrElse(""), // will have the '/' if presente
          Option(query).getOrElse(""), // will have the '?' if presente
        )
      )
    case _: String => Left(FailToParse(s"MUST be DID URL with no fragment '$did'"))
  }
}

opaque type FROMTO = String
object FROMTO {
  extension (id: FROMTO)
    inline def value: String = id
    inline def asTO = TO.unsafe_apply(id)
    inline def asFROM = FROM.unsafe_apply(id)
    inline def toDID = DIDURL.parseString(id).toDID
    inline def toDIDSubject: DIDSubject = DIDSubject(toDID.did)

  private[did] inline def unsafe_apply(s: String): FROMTO = s
  def apply(s: String): FROMTO = s.tap(e => FromTo.unsafe_parse(e))
  def either(s: String): Either[FailToParse, FROMTO] = FromTo.either_parse(s).flatMap(fromDIDURL(_))
  def fromDIDURL(s: DIDURL): Either[FailToParse, FROMTO] =
    if (s.fragment.isEmpty) Right(unsafe_apply(s.string))
    else Left(FailToParse(s"MUST be DID URL with no fragment '$s'"))
  def fromForcedDIDURL(s: DIDURL) = s.toFROMTO // REMOVE
  /** FIXME REMOVE method @throws AssertionError if not a valid DIDSubject */
  def force(s: String) = fromForcedDIDURL(DIDURL.parseString(s))

  given decoder: JsonDecoder[FROMTO] = JsonDecoder.string.mapOrFail(s => FROMTO.either(s).left.map(_.error))
  given encoder: JsonEncoder[FROMTO] = JsonEncoder.string.contramap(e => e.value)
  // These given are useful if we use the FROMTO as a Key (ex: Map[FROMTO , Value])
  given JsonFieldDecoder[FROMTO] = JsonFieldDecoder.string.mapOrFail(s => FROMTO.either(s).left.map(_.error))
  given JsonFieldEncoder[FROMTO] = JsonFieldEncoder.string.contramap(e => e.value)
}

/** FROM is a DID URL with Path and Query parameter (that can be missing) (no-fragment)
  *
  * @note
  *   the class TO is just a copy of this class and replace FROM with TO
  */
opaque type FROM = String
object FROM {
  extension (id: FROM)
    inline def value: String = id
    inline def namespace: String = FromTo.unsafe_parse(id).namespace
    inline def didSyntax: String = FromTo.unsafe_parse(id).didSyntax
    inline def path: String = FromTo.unsafe_parse(id).path
    inline def query: String = FromTo.unsafe_parse(id).query
    inline def toDID = DIDURL.parseString(id).toDID
    inline def toDIDSubject: DIDSubject = DIDSubject(toDID.did)
    inline def asDIDURL = DIDURL(namespace = id.namespace, didSyntax = id.didSyntax, path = id.path, query = id.query)
    inline def asTO = TO.unsafe_apply(id)
    inline def asFROMTO = FROMTO.unsafe_apply(id)

  /** Like apply but for optimization (no runtime code hopefully) */
  private[did] inline def unsafe_apply(s: String): FROM = s

  /** @throws AssertionError
    *   if not a valid DIDSubjectQ
    */
  def apply(s: String): FROM = s.tap(e => FromTo.unsafe_parse(e)) // 'tap' is to throws as soon as possible

  def either(s: String): Either[FailToParse, FROM] = FromTo.either_parse(s).flatMap(fromDIDURL(_))
  def fromDIDURL(s: DIDURL): Either[FailToParse, FROM] =
    if (s.fragment.isEmpty) Right(unsafe_apply(s.string))
    else Left(FailToParse(s"MUST be DID URL with no fragment '$s'"))
  given decoder: JsonDecoder[FROM] = JsonDecoder.string.mapOrFail(s => FROM.either(s).left.map(_.error))
  given encoder: JsonEncoder[FROM] = JsonEncoder.string.contramap(e => e.value)
}

/** TO is a DID URL with Path and Query parameter (that can be missing) (no-fragment)
  *
  * @note
  *   the class FROM is just a copy of this class and replace TO with FROM
  */
opaque type TO = String
object TO {

  extension (id: TO)
    inline def value: String = id
    inline def namespace: String = FromTo.unsafe_parse(id).namespace
    inline def didSyntax: String = FromTo.unsafe_parse(id).didSyntax
    inline def path: String = FromTo.unsafe_parse(id).path
    inline def query: String = FromTo.unsafe_parse(id).query
    inline def toDID = DIDURL.parseString(id).toDID
    inline def toDIDSubject: DIDSubject = DIDSubject(toDID.did)
    inline def asDIDURL = DIDURL(namespace = id.namespace, didSyntax = id.didSyntax, path = id.path, query = id.query)
    inline def asFROM = FROM.unsafe_apply(id)
    inline def asFROMTO = FROMTO.unsafe_apply(id)

  /** Like apply but for optimization (no runtime code hopefully) */
  private[did] inline def unsafe_apply(s: String): TO = s

  /** @throws AssertionError
    *   if not a valid DIDSubjectQ
    */
  def apply(s: String): TO = s.tap(e => FromTo.unsafe_parse(e)) // 'tap' is to throws as soon as possible
  def either(s: String): Either[FailToParse, TO] = FromTo.either_parse(s).flatMap(fromDIDURL(_))
  def fromDIDURL(s: DIDURL): Either[FailToParse, TO] =
    if (s.fragment.isEmpty) Right(unsafe_apply(s.string))
    else Left(FailToParse(s"MUST be DID URL with no fragment '$s'"))

  given decoder: JsonDecoder[TO] = JsonDecoder.string.mapOrFail(s => TO.either(s).left.map(_.error))
  given encoder: JsonEncoder[TO] = JsonEncoder.string.contramap(e => e.value)
}
