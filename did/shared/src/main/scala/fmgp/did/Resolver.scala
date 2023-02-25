package fmgp.did

import zio._

import fmgp.crypto.error.DidFail
import fmgp.did.comm.{FROM, FROMTO, TO}

/** @see
  *   https://w3c.github.io/did-spec-registries/#did-methods
  * @see
  *   https://www.w3.org/TR/did-spec-registries/
  */
trait Resolver {
  def didDocument(did: FROMTO | TO | FROM): IO[DidFail, DIDDocument] =
    didDocumentOf(did.asInstanceOf[FROMTO])
  protected def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument]
}

case class MultiResolver(
    firstResolver: Resolver,
    remainResolvers: Resolver*,
) extends Resolver {

  override protected def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument] =
    ZIO.raceAll(firstResolver.didDocument(did), remainResolvers.map(_.didDocument(did)))
    // ZIO.firstSuccessOf(
    //   zio = Console.printLine("firstResolver").orDie *> firstResolver.didDocument(did),
    //   rest = remainResolvers.map(Console.printLine("remainResolvers").orDie *> _.didDocument(did))
    // )
}
