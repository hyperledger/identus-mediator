package fmgp.did

import munit._
import zio.json._
import fmgp.did.DIDDocument

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
    }
  }

}
