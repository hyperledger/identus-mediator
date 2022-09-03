package fmgp.resolver.peer

import munit._
import zio._
import zio.json._
import fmgp.did.resolver.peer.DIDPeer

import DIDPeerExamples._

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

  testZ("testZ") {
    for {
      _ <- ZIO.unit
    } yield ()
  }

}
