package fmgp.did

import munit._
import zio.json._
import zio.json.ast.{Json, JsonCursor}
import fmgp.did.DIDDocument

class DIDServiceSuite extends FunSuite {

  test("Example 20 parse") {
    val json = DIDExamples.EX20.fromJson[ast.Json]
    val cursor = JsonCursor.field("service").isArray.element(0)
    val ret = json.flatMap(_.get(cursor)).flatMap(_.toJson.fromJson[DIDService])
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, DIDExamples.EX20_DIDService)
    }
  }

}
