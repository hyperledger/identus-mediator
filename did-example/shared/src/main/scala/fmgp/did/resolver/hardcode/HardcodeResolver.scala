package fmgp.did.resolver.hardcode

import zio._
import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

class HardcodeResolver extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument] =
    HardcodeResolver.didDocumentOf(did)
}

object HardcodeResolver {
  val default = new HardcodeResolver()
  val layer: ULayer[Resolver] = ZLayer.succeed(default)
  val layerHardcodeResolver: ULayer[HardcodeResolver] = ZLayer.succeed(default)

  def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument] = did.value match
    case "did:example:alice" => ZIO.succeed(DidExample.senderDIDDocument)
    case "did:example:bob"   => ZIO.succeed(DidExample.recipientDIDDocument)
    case _                   => ZIO.fail(DIDSubjectNotSupported(did.toDID))
}
