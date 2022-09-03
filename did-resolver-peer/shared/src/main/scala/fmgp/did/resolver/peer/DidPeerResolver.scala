package fmgp.did.resolver.peer

import zio._
import fmgp.did._

/** DID Peer
  *
  * @see
  *   https://identity.foundation/peer-did-method-spec/#method-specific-identifier
  */
sealed trait DIDPeer extends DID {
  def namespace: String = "peer"
  def specificId: String = numalgo + transform + encnumbasis
  def numalgo: 0 | 1 | 2
  def transform = 'z'
  def encnumbasis: Array46_BASE58BTC = "46*BASE58BTC"

  type Array46_BASE58BTC = String // FIXME

}

trait DIDPeer0 extends DIDPeer
trait DIDPeer1 extends DIDPeer
trait DIDPeer2 extends DIDPeer {
  def numalgo: 2 = 2
  def element: String = "." + "( purposecode transform encnumbasis / service )"
  type Purposecode = 'A' | 'E' | 'V' | 'I' | 'D' | 'S'
  // keypurpose = WTF is this?
  type Service = C1_B64URL

  type C1_B64URL = String
}

object DIDPeer {
  def regexPeer =
    "^did:peer:(([01](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))|(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)?)))$".r
  def regexPeer0 = "^did:peer:(0(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  def regexPeer1 = "^did:peer:(1(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  def regexPeer2 = "^did:peer:(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)?))$".r
}

object DidPeerResolver extends Resolver {
  override def didDocument(did: DID): RIO[Error, DIDDocument] = did match {
    case peer: DIDPeer => didDocument(peer)
    case _             => ZIO.fail(NotImplementedError())
  }

  /** see https://identity.foundation/peer-did-method-spec/#generation-method */
  def didDocument(didPeer: DIDPeer): UIO[DIDDocument] = didPeer match {
    case peer: DIDPeer0 => genesisDocument(peer)
    case peer: DIDPeer1 => genesisDocument(peer)
    case peer: DIDPeer2 => genesisDocument(peer)
  }

  def genesisDocument(did: DIDPeer0): UIO[DIDDocument] = ???
  def genesisDocument(did: DIDPeer1): UIO[DIDDocument] = ???
  def genesisDocument(did: DIDPeer2): UIO[DIDDocument] = ???
}
