package fmgp.did

import zio._

trait Resolver {
  def didDocument(did: DID): RIO[Error, DIDDocument]
}
