package fmgp.did.resolver.uniresolver

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.resolver._
import fmgp.crypto.error._

import scala.util.chaining.scalaUtilChainingOps
import scala.scalajs.js
import org.scalajs.dom._

object Uniresolver {
  val default = Uniresolver("https://dev.uniresolver.io/1.0/identifiers/")
  val layerDefault: ULayer[Resolver] = ZLayer.succeed(default)
}

case class Uniresolver(uniresolverServer: String) extends Resolver {

  override protected def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument] = {
    // if (!methods.contains(did.toDID.namespace)) ZIO.fail(DidMethodNotSupported(did.toDID.namespace))
    // else
    val url = uniresolverServer + did
    val program = for {
      data <- ZIO
        .fromPromiseJS(fetch(url, new RequestInit { method = HttpMethod.GET }))
        .flatMap(e => ZIO.fromPromiseJS(e.text()))
        .catchAll(ex => ZIO.fail(SomeThrowable(ex)))
      didResolutionResult <- data.fromJson[DIDResolutionResult] match
        case Left(error)  => ZIO.fail(FailToParse(error))
        case Right(value) => ZIO.succeed(value.didDocument)
    } yield (didResolutionResult)

    program
  }

}
