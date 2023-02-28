package fmgp.did.comm

import munit._

import zio._
import zio.json._
import zio.json.ast.Json

/** didJVM/testOnly fmgp.did.comm.EnumTypesSuite */
class EnumTypesSuite extends FunSuite {
  test("Parse ReturnRoute 'all'") {
    "\"all\"".fromJson[ReturnRoute] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, ReturnRoute.all)
    }
  }

  test("Parse ReturnRoute 'thread'") {
    "\"thread\"".fromJson[ReturnRoute] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, ReturnRoute.thread)
    }
  }
}
