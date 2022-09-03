package fmgp.did

import zio._

/** @see
  *   https://w3c.github.io/did-spec-registries/#did-methods
  * @see
  *   https://www.w3.org/TR/did-spec-registries/
  */
trait Resolver {
  def didDocument(did: DID): RIO[Error, DIDDocument]
}
