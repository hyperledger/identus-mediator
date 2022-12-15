package fmgp.did.resolver.peer

import munit._
import zio._
import zio.json._
import fmgp.did._
import fmgp.did.resolver.peer._
import fmgp.multibase._

import DIDPeerExamples._
import fmgp.did.resolver.peer.DidPeerResolver
import zio.json.ast.Json

/** didResolverPeerJVM/testOnly fmgp.did.resolver.peer.DIDPeerSuite */
class DIDPeerSuite extends ZSuite {

  test("Check regex for peer (method 1)") {
    val d = ex5_peer1
    assert(DIDPeer.regexPeer.matches(d))
    assert(!DIDPeer.regexPeer0.matches(d))
    assert(DIDPeer.regexPeer1.matches(d))
    assert(!DIDPeer.regexPeer2.matches(d))
  }

  test("Check regex for peer (method 2)") {
    val d = ex4_peer2_did
    assert(DIDPeer.regexPeer.matches(d))
    assert(!DIDPeer.regexPeer0.matches(d))
    assert(!DIDPeer.regexPeer1.matches(d))
    assert(DIDPeer.regexPeer2.matches(d))
  }

  test("Create DIDPeer apply ex5_peer1") {
    val s = DIDSubject(ex5_peer1)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did) =>
        assertEquals(did, DIDPeer1("zQmZMygzYqNwU6Uhmewx5Xepf2VLp5S4HLSwwgf2aiKZuwa"))
        assertEquals(did.string, s.string)
  }

  test("Create DIDPeer apply ex4_peer2_did") {
    val s = DIDSubject(ex4_peer2_did)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did) =>
        assertEquals(
          did,
          DIDPeer2(
            Seq(
              DIDPeer2.ElementE(Multibase("z6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc")),
              DIDPeer2.ElementV(Multibase("z6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V")),
              DIDPeer2.ElementV(Multibase("z6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg")),
              DIDPeer2.ElementService(
                "eyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0"
              ),
            )
          )
        )
        assertEquals(did.string, s.string)
  }

  test("Create DIDPeer apply myExampleDID") {
    val s = DIDSubject(myExampleDID)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did) =>
        assertEquals(
          did,
          DIDPeer2(
            Seq(
              DIDPeer2.ElementE(Multibase("z6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE")),
              DIDPeer2.ElementV(Multibase("z6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY")),
              DIDPeer2.ElementService(
                "eyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19"
              ),
            )
          )
        )

        assertEquals(did.string, s.string)
        assertEquals(
          Some(did.document.toJsonPretty),
          myExampleDIDDocument.fromJson[Json].map(_.toJsonPretty).toOption
        )
  }

  // testZ("DidPeerResolver.didDocument") {

  //   // val exDID = DIDSubject(DIDPeerExamples.ex4_peer2_did)

  //   DIDPeer.fromDID(exDID)
  //   for {
  //     doc <- DidPeerResolver.didDocument(exDID)
  //     _ = println(doc)
  //   } yield ()
  // }

}
