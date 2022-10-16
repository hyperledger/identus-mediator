package fmgp.did

import zio._

import fmgp.crypto.error.DidMethodNotSupported

/** @see
  *   https://w3c.github.io/did-spec-registries/#did-methods
  * @see
  *   https://www.w3.org/TR/did-spec-registries/
  */
trait Resolver {
  def didDocument(did: DIDSubject): IO[DidMethodNotSupported, DIDDocument] // FIXME Error type
}
