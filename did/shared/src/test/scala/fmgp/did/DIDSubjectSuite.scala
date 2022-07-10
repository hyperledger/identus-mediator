package fmgp.did

import munit._
import zio.json._
import fmgp.did.DIDSubject._

class DIDSubjectSuite extends FunSuite {
  val str = "did:example:123456789abcdefghijk"

  test("DIDSubject apply") {
    val ret = DIDSubject(str)
    assert(ret.isInstanceOf[DIDSubject])
    assert(ret.toDID.isInstanceOf[DID])
  }

  test("DIDSubject as DID") {
    val ret = DIDSubject(str)
    assert((ret: DID).isInstanceOf[DID])
    assertEquals(ret.scheme, "did")
    assertEquals(ret.namespace, "example")
    assertEquals(ret.specificId, "123456789abcdefghijk")
  }

  test("DIDSubject parse") {
    val ret = s"\"$str\"".fromJson[DIDSubject]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj.value, str)
    }
  }

  test("DIDSubject serialize & parse") {
    val ret = DIDSubject(str).toJson
    assertEquals(ret, s"\"$str\"")
  }
}
