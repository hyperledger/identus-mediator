package fmgp.did

import munit._
import zio.json._
import fmgp.did.DIDDocument
import fmgp.crypto._

class DIDDocumentSuite extends FunSuite {

  test("Example 10 parse") {
    val ret = DIDExamples.EX10.fromJson[DIDDocument]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, DIDExamples.EX10_DIDDocument)
    }
  }

  test("Example 10 serialize & parse") {
    val json = DIDExamples.EX10_DIDDocument.toJson
    val obj = json.fromJson[DIDDocument]
    assertEquals(obj, Right(DIDExamples.EX10_DIDDocument))
  }

  test("Example 17 parse") {
    val ret = DIDExamples.EX17.fromJson[DIDDocument]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, DIDExamples.EX17_DIDDocument)
      // assertEquals(obj.toJsonPretty, DIDExamples.EX17.fromJson[zio.json.ast.Json].toOption.get.toJsonPretty)
    }
  }

  test("parse and stringify an VerificationMethod example") {
    val aux: VerificationMethod = VerificationMethodEmbeddedJWK(
      id = "did:example:123456789abcdefghi#keys-1",
      controller = "did:example:123456789abcdefghi",
      `type` = "Ed25519VerificationKey2020",
      publicKeyJwk = OKPPublicKey(kty = KTY.OKP, crv = Curve.X25519, x = "Test", kid = None)
    )
    val expected = """{
      |  "id" : "did:example:123456789abcdefghi#keys-1",
      |  "controller" : "did:example:123456789abcdefghi",
      |  "type" : "Ed25519VerificationKey2020",
      |  "publicKeyJwk" : {
      |    "kty" : "OKP",
      |    "crv" : "X25519",
      |    "x" : "Test"
      |  }
      |}""".stripMargin
    assertEquals(aux.toJsonPretty, expected)
  }

}
