// package fmgp.crypto

// import munit._
// import zio.json._
// import fmgp.did.DIDDocument

// class JWKSuite extends FunSuite {

// FIXME new class ECKey
// test("JwkCruve test") {
//   val ret = JWKExamples.senderSecp256k1.fromJson[JWKCruve]
//   ret match {
//     case Left(error) => fail(error)
//     case Right(obj) =>
//       assertEquals(obj.kid, Some("did:example:alice#key-3"))
//       assertEquals(obj.kty, "EC")
//       assertEquals(obj.d, "N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA")
//       assertEquals(obj.x, Some("aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk"))
//       assertEquals(obj.y, Some("JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"))

//   }
// }

// }
