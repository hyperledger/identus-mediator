package fmgp.did.comm.mediator

import zio._

import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto.error._

final case class DynamicResolver(anotherResolver: Resolver, didSocketManager: Ref[DIDSocketManager]) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[DidMethodNotSupported, DIDDocument] =
    for {
      cleanDoc <- anotherResolver.didDocument(did)
      sm <- didSocketManager.get

      doc = DIDDocumentClass(
        id = cleanDoc.id,
        alsoKnownAs = cleanDoc.alsoKnownAs,
        controller = cleanDoc.controller,
        verificationMethod = cleanDoc.verificationMethod,
        authentication = cleanDoc.authentication,
        assertionMethod = cleanDoc.assertionMethod,
        keyAgreement = cleanDoc.keyAgreement,
        capabilityInvocation = cleanDoc.capabilityInvocation,
        capabilityDelegation = cleanDoc.capabilityDelegation,
        service = cleanDoc.service, // FIXME TODO
      )
    } yield (doc)
}
