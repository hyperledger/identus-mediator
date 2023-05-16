package fmgp.did.comm.mediator

import zio._

import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto.error._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.uniresolver.Uniresolver

final case class DynamicResolver(
    resolver: Resolver,
    didSocketManager: Ref[DIDSocketManager],
) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument] =
    for {
      docFromResolver <- resolver.didDocument(did)
      sm <- didSocketManager.get
      doc = DIDDocumentClass(
        id = docFromResolver.id,
        alsoKnownAs = docFromResolver.alsoKnownAs,
        controller = docFromResolver.controller,
        verificationMethod = docFromResolver.verificationMethod,
        authentication = docFromResolver.authentication,
        assertionMethod = docFromResolver.assertionMethod,
        keyAgreement = docFromResolver.keyAgreement,
        capabilityInvocation = docFromResolver.capabilityInvocation,
        capabilityDelegation = docFromResolver.capabilityDelegation,
        service = docFromResolver.service, // TODO data from sm
      )
    } yield (doc)
}

object DynamicResolver {
  def resolverLayer(didSocketManager: Ref[DIDSocketManager]): ZLayer[Any, Nothing, DynamicResolver] =
    ZLayer.succeed(DynamicResolver(DidPeerResolver.default, didSocketManager))
}
