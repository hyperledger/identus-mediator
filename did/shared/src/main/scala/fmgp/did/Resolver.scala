package fmgp.did

import zio._

import fmgp.crypto.error.DidMethodNotSupported
import fmgp.did.comm.{FROM, FROMTO, TO}

/** @see
  *   https://w3c.github.io/did-spec-registries/#did-methods
  * @see
  *   https://www.w3.org/TR/did-spec-registries/
  */
trait Resolver {
  // FIXME Error type
  def didDocument(did: FROMTO | TO | FROM): IO[DidMethodNotSupported, DIDDocument] =
    didDocumentOf(did.asInstanceOf[FROMTO])
  protected def didDocumentOf(did: FROMTO): IO[DidMethodNotSupported, DIDDocument]
}
