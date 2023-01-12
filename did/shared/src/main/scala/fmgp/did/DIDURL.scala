package fmgp.did

object DIDURL {
  val pattern = """^did:([^\s:]+):([^/\?\#\s]*)([^\?\#\s]*)(\?[^\#\s:]*)?(\#.*)?$""".r
  // ------------------|--method-|------id-----|----path---|---query----|-fragment

  /** @throws AssertionError if not a valid DIDSubject */
  inline def parseString(id: String) = id match {
    case pattern(namespace, subject, path, query, fragment) =>
      DIDURL(
        namespace,
        subject,
        Option(path).getOrElse(""),
        Option(query).getOrElse(""),
        Option(fragment).getOrElse("")
      )
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
    path: PathAbempty = "",
    query: Query = "",
    fragment: Fragment = "",
) { self =>
  def specificId: String = didSyntax + path + query + fragment
  def string = s"did:$namespace:$specificId"
  def toDID: DID = new {
    val namespace = self.namespace
    val specificId = self.specificId
  }
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
