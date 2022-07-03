package fmgp.did

import munit._
import zio.json._
import fmgp.did.DIDDocument

class DIDDocumentSuite extends FunSuite {

  test("Example 10 parse") {
    val ret = Examples.EX10.fromJson[DIDDocument]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, Examples.EX10_DIDDocument)
    }
  }

  test("Example 10 serialize parse") {
    val json = Examples.EX10_DIDDocument.toJson
    val obj = json.fromJson[DIDDocument]
    assertEquals(obj, Right(Examples.EX10_DIDDocument))
  }

  test("Example 17 parse") {
    val ret = Examples.EX17.fromJson[DIDDocument]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, Examples.EX17_DIDDocument)
    }
  }

}
