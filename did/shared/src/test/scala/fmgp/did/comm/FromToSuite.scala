package fmgp.did.comm

import munit._
import zio.json._

import fmgp.did._

/** didJVM/testOnly fmgp.did.comm.FromToSuite */
class FromToSuite extends FunSuite {

  val did = "did:example:123456789abcdefghijk"
  val didWithPath = "did:example:123456789abcdefghijk/path"
  val didWithQuery = "did:example:123456789abcdefghijk?a=1"
  val didWithQueryEmpty = "did:example:123456789abcdefghijk?"

  val all = Seq(did, didWithPath, didWithQuery, didWithQueryEmpty)

  // ################
  // ###  FROM&TO  ###
  // ################

  test("FROM&TO apply did") {
    val to = TO(did)
    assert(to.isInstanceOf[TO])
    assertEquals(to.didSyntax, "123456789abcdefghijk")
    val from = FROM(did)
    assert(from.isInstanceOf[FROM])
    assertEquals(from.didSyntax, "123456789abcdefghijk")
  }
  test("FROM&TO apply didWithPath") {
    val to = TO(didWithPath)
    assert(to.isInstanceOf[TO])
    assertEquals(to.didSyntax, "123456789abcdefghijk")
    assertEquals(to.path, "/path")
    val from = FROM(didWithPath)
    assert(from.isInstanceOf[FROM])
    assertEquals(from.didSyntax, "123456789abcdefghijk")
    assertEquals(from.path, "/path")
  }
  test("FROM&TO apply didWithQuery") {
    val to = TO(didWithQuery)
    assert(to.isInstanceOf[TO])
    assertEquals(to.didSyntax, "123456789abcdefghijk")
    assertEquals(to.query, "?a=1")
    val from = FROM(didWithQuery)
    assert(from.isInstanceOf[FROM])
    assertEquals(from.didSyntax, "123456789abcdefghijk")
    assertEquals(from.query, "?a=1")
  }
  test("FROM&TO apply didWithQueryEmpty") {
    val to = TO(didWithQueryEmpty)
    assert(to.isInstanceOf[TO])
    assertEquals(to.query, "?")
    val from = FROM(didWithQueryEmpty)
    assert(from.isInstanceOf[FROM])
    assertEquals(from.query, "?")
  }
  test("FROM&TO to DID") {
    val to = TO(did).asDIDURL.toDID
    assert(to.isInstanceOf[DID])
    assertEquals(to.scheme, "did")
    assertEquals(to.namespace, "example")
    assertEquals(to.specificId, "123456789abcdefghijk")
    val from = TO(did).asDIDURL.toDID
    assert(from.isInstanceOf[DID])
    assertEquals(from.scheme, "did")
    assertEquals(from.namespace, "example")
    assertEquals(from.specificId, "123456789abcdefghijk")
  }

  all.foreach { str =>
    test("FROM&TO parse") {
      s"\"$str\"".fromJson[TO] match {
        case Left(error) => fail(error)
        case Right(obj)  => assertEquals(obj.value, str)
      }
      s"\"$str\"".fromJson[FROM] match {
        case Left(error) => fail(error)
        case Right(obj)  => assertEquals(obj.value, str)
      }
    }

    test("FROM&TO serialize & parse") {
      val ret = TO(str).toJson
      assertEquals(ret, s"\"$str\"")
    }
  }

  // MUST parse FROM
  Seq(
    """did:m:s""",
    """did:m:s/""",
    """did:m:s/p""",
    """did:m:s/p/""",
    """did:m:s?q""",
    """did:m:s?""",
    """did:m:s????""",
  ).foreach { did =>
    test(s"FROM&TO apply parse '$did'") {
      val to = TO(did)
      assert(to.isInstanceOf[TO])
      val from = FROM(did)
      assert(from.isInstanceOf[FROM])
    }
  }

  Seq(
    """did::""",
    """did::i""",
    """did:m:""",
    """did:m:i#""",
    """did:m:i?f#""",
    """did:m:s?q#f""",
    """did:m:s#""",
    """did:m:s#f""",
    """did:m:s/p?q#f""",
  ).foreach { invalidDID =>
    test(s"FROM&TO apply MUST fail for '$invalidDID'") {
      interceptMessage[java.lang.AssertionError](s"MUST be DID URL with no fragment '$invalidDID'") {
        TO(invalidDID)
      }
      interceptMessage[java.lang.AssertionError](s"MUST be DID URL with no fragment '$invalidDID'") {
        FROM(invalidDID)
      }
    }
  }

}
