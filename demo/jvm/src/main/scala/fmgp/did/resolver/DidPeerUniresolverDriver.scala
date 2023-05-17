package fmgp.did.method

import zio._
import zio.json._
import zio.json.ast.Json
import zio.http._
import zio.http.model._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.comm.FROMTO
import fmgp.crypto.error.DidFail
import fmgp.did.DIDDocument
import fmgp.did.uniresolver.DIDResolutionResult

object DidPeerUniresolverDriver {

  val resolverPeer: Http[DidPeerResolver, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.GET -> !! / "resolver" / "peer" / did =>
      for {
        resolver <- ZIO.service[DidPeerResolver]
        response <-
          ZIO
            .fromEither(FROMTO.either(did))
            .flatMap(resolver.didDocument(_))
            .map(didDocument =>
              DIDResolutionResult(
                didDocument = didDocument,
                didResolutionMetadata = Json.Obj(),
                didDocumentMetadata = Json.Obj(),
              )
            )
            .map(didResolutionResult =>
              Response(
                status = Status.Ok,
                headers = Headers(HeaderNames.contentType, HeaderValues.applicationJson),
                body = Body.fromCharSequence(didResolutionResult.toJsonPretty),
              )
            )
            .catchAll(didFail =>
              ZIO.succeed(
                Response(
                  status = Status.BadRequest,
                  headers = Headers.empty,
                  body = Body.fromCharSequence(didFail.toJsonPretty),
                )
              )
            )
      } yield (response)
    }
}
