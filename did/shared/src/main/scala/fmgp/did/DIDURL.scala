package fmgp.did

object DIDURL {
  val pattern = """^did:([^\s:]+):([^/\?\#\s]*)([^\?\#\s]*)(\?[^\#\s:]*)?(\#.*)?$""".r
  // ------------------|--method-|------id-----|----path---|---query----|-fragment

  /** @throws AssertionError if not a valid DIDSubject */
  inline def parseString(id: String) = id match {
    case pattern(namespace, subject, path, null, null) =>
      DIDURL(namespace, subject, if (path.isEmpty) None else Some(path), None, None)
    case pattern(namespace, subject, path, null, fragment) =>
      DIDURL(namespace, subject, if (path.isEmpty) None else Some(path), None, Some(fragment.drop(1)))
    case pattern(namespace, subject, path, query, null) =>
      DIDURL(namespace, subject, if (path.isEmpty) None else Some(path), Some(query.drop(1)), None)
    case pattern(namespace, subject, path, query, fragment) =>
      DIDURL(namespace, subject, if (path.isEmpty) None else Some(path), Some(query.drop(1)), Some(fragment.drop(1)))
    case _ => throw new java.lang.AssertionError(s"Fail to parse DIDSubjectQ: '$id'")
  }
}

/** DIDURL
  *
  * did-url = did path-abempty [ "?" query ] [ "#" fragment ]
  *
  * @see
  *   https://www.w3.org/TR/did-core/#did-url-syntax
  */
case class DIDURL(
    val namespace: String,
    didSyntax: DIDSyntax,
    path: Option[PathAbempty] = None,
    query: Option[Query] = None,
    fragment: Option[Fragment] = None,
) extends DID {

  override def specificId: String =
    didSyntax +
      path.map("/" + _).getOrElse("") +
      query.map("?" + _).getOrElse("") +
      fragment.map("#" + _).getOrElse("")
}

/** @see
  *   https://www.rfc-editor.org/rfc/rfc3986#section-3.3
  *
  * {{{
  * path          = path-abempty    ; begins with "/" or is empty
  *               / path-absolute   ; begins with "/" but not "//"
  *               / path-noscheme   ; begins with a non-colon segment
  *               / path-rootless   ; begins with a segment
  *               / path-empty      ; zero characters
  * path-abempty  = *( "/" segment )
  * path-absolute = "/" [ segment-nz *( "/" segment ) ]
  * path-noscheme = segment-nz-nc *( "/" segment )
  * path-rootless = segment-nz *( "/" segment )
  * path-empty    = 0<pchar>
  * }}}
  */
type PathAbempty = String

/** @see
  *   https://www.rfc-editor.org/rfc/rfc3986#section-3.4
  *
  * {{{
  * query       = *( pchar / "/" / "?" )
  * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
  * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~""
  * pct-encoded = "%" HEXDIG HEXDIG
  * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
  * }}}
  */
type Query = String

/** @see
  *   https://www.rfc-editor.org/rfc/rfc3986#section-3.5
  *
  * {{{
  * query       = *( pchar / "/" / "?" )
  * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
  * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~""
  * pct-encoded = "%" HEXDIG HEXDIG
  * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
  * }}}
  */
type Fragment = String
