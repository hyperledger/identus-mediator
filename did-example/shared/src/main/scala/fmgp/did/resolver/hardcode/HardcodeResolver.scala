package fmgp.did.method.hardcode

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
    // TODO use this // case "did:example:alice"     => ZIO.succeed(DidExample.senderDIDDocument)
    // TODO use this // case "did:example:bob"       => ZIO.succeed(DidExample.recipientDIDDocument)
    case "did:example:alice"     => ZIO.succeed(DidExampleSicpaRustAlice.aliceDIDDocument)
    case "did:example:bob"       => ZIO.succeed(DidExampleSicpaRustBob.bobDIDDocument)
    case "did:example:charlie"   => ZIO.succeed(DidExampleSicpaRustCharlie.charlieDIDDocument)
    case "did:example:mediator1" => ZIO.succeed(DidExampleSicpaRustMediator1.mediator1DIDDocument)
    case "did:example:mediator2" => ZIO.succeed(DidExampleSicpaRustMediator2.mediator2DIDDocument)
    case "did:example:mediator3" => ZIO.succeed(DidExampleSicpaRustMediator3.mediator3DIDDocument)
    case _                       => ZIO.fail(DIDSubjectNotSupported(did.toDID))
}
