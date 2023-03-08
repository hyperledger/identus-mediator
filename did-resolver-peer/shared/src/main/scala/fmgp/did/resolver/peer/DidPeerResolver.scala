package fmgp.did.resolver.peer

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._
import fmgp.crypto.error.DidMethodNotSupported

class DidPeerResolver extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[DidMethodNotSupported, DIDDocument] = did.toDID match {
    case peer: DIDPeer => DidPeerResolver.didDocument(peer)
    case did if DIDPeer.regexPeer.matches(did.string) =>
      DidPeerResolver.didDocument(DIDPeer(did))
    case did => ZIO.fail(DidMethodNotSupported(did.namespace))
  }
}
object DidPeerResolver {
  val default = new DidPeerResolver()
  val layer: ULayer[Resolver] = ZLayer.succeed(default)
  val layerDidPeerResolver: ULayer[DidPeerResolver] = ZLayer.succeed(default)

  /** see https://identity.foundation/peer-did-method-spec/#generation-method */
  def didDocument(didPeer: DIDPeer): UIO[DIDDocument] = didPeer match {
    case peer: DIDPeer0 => genesisDocument(peer)
    case peer: DIDPeer1 => genesisDocument(peer)
    case peer: DIDPeer2 => genesisDocument(peer)
  }

  def genesisDocument(did: DIDPeer0): UIO[DIDDocument] = ZIO.succeed(did.document)
  def genesisDocument(did: DIDPeer1): UIO[DIDDocument] = ZIO.succeed(did.document)
  def genesisDocument(did: DIDPeer2): UIO[DIDDocument] = ZIO.succeed(did.document)
}
