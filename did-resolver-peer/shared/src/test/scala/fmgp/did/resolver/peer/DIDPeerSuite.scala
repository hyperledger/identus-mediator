package fmgp.did.resolver.peer

import munit._
import zio._
import zio.json._
import fmgp.crypto._
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

  test("DID peer - missing optional fields of the service endpoint") {
    val s = DIDSubject(rootsid_ex_peer2_did)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did) =>
        assertEquals(did.string, s.string)
        assertEquals(
          Some(did.document.toJsonPretty),
          rootsid_ex_peer2_didDocument.fromJson[Json].map(_.toJsonPretty).toOption
        )
  }

  test("Create DIDPeer apply keys") {
    val keyAgreement = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.X25519,
      d = "9yAs1ddRaUq4d7_HfLw2VSj1oW2kirb2wALmPXrRuZA",
      x = "xfvZlkAnuNpssHOR2As4kUJ8zEPbowOIU5VbhBsYoGo",
      kid = None // : Option[String]
    )
    val keyAuthentication = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = "-yjzvLY5dhFEuIsQcebEejbLbl3b8ICR7b2y2_HqFns",
      x = "vfzzx6IIWdBI7J4eEPHuxaXGErhH3QXnRSQd0d_yn0Y",
      kid = None // : Option[String]
    )
    val obj =
      DIDPeer2(Seq(keyAgreement, keyAuthentication), Seq(DIDPeerServiceEncoded("http://localhost:8080")))

    assertEquals(
      obj,
      DIDPeer(
        DIDSubject(
          "did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.Vz6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
        )
      )
    )
  }

  test("Check service's Base64 - decode and encode (did must not change)") {
    def testDid(s: String) = s"did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.S$s"

    /** {"r":[],"s":"http://localhost:8080","a":["didcomm\/v2"],"t":"dm"} */
    val service = "eyJyIjpbXSwicyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImEiOlsiZGlkY29tbVwvdjIiXSwidCI6ImRtIn0="

    /** Default {"t":"dm","s":"http://localhost:8080","r":[],"a":["didcomm/v2"]} */
    val defaultService = "eyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"

    DIDPeer(DIDSubject(testDid(service))) match
      case DIDPeer0(encnumbasis) => fail("Wrong DIDPeer type")
      case DIDPeer1(encnumbasis) => fail("Wrong DIDPeer type")
      case obj @ DIDPeer2(elements) =>
        assertEquals(obj.did, testDid(service))
        assertNotEquals(obj.did, testDid(defaultService))
  }

}
