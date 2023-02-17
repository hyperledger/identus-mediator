package fmgp.did

import munit._
import zio.json._
import fmgp.did.DIDSubject._

/** didJVM/testOnly fmgp.did.DIDSubjectSuite */
class DIDSubjectSuite extends FunSuite {

  val did = "did:example:123456789abcdefghijk"
  val didWithQuery = "did:example:123456789abcdefghijk?a=1"
  val didWithQueryEmpty = "did:example:123456789abcdefghijk?"

  // ####################
  // ###  DIDSubject  ###
  // ####################

  test("DIDSubject apply") {
    val ret = DIDSubject(did)
    assert(ret.isInstanceOf[DIDSubject])
    assert(ret.toDID.isInstanceOf[DID])
  }

  // MUST parse DIDSubjectQ
  Seq(
    """did:m:s""",
    """did:m:s/""",
    """did:m:s/p""",
    """did:m:s/p/""",
  ).foreach { did =>
    test(s"DIDSubject apply parse '$did'") {
      val ret = DIDSubject(did)
      assert(ret.isInstanceOf[DIDSubject])
      assert(ret.toDID.isInstanceOf[DID])
    }
  }

  Seq(
    "did::",
    "did::i",
    "did:m:",
    "did:m:i#",
    "did:m:i?f",
  ).foreach { invalidDID =>
    test(s"DIDSubject apply MUST fail for '$invalidDID'") {
      interceptMessage[java.lang.AssertionError](s"Fail to parse DIDSubject: '$invalidDID'") {
        DIDSubject(invalidDID)
      }
    }
  }

  test("DIDSubject as DID") {
    val ret = DIDSubject(did)
    assert((ret: DID).isInstanceOf[DID])
    assertEquals(ret.scheme, "did")
    assertEquals(ret.namespace, "example")
    assertEquals(ret.specificId, "123456789abcdefghijk")
  }

  test("DIDSubject parse") {
    val ret = s"\"$did\"".fromJson[DIDSubject]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj.did, did)
    }
  }

  test("DIDSubject serialize & parse") {
    val ret = DIDSubject(did).toJson
    assertEquals(ret, s"\"$did\"")
  }

  // #####################
  // ###  DIDSubjectQ  ###
  // #####################

  test("DIDSubjectQ apply") {
    val ret = DIDSubjectQ(didWithQuery)
    assert(ret.isInstanceOf[DIDSubjectQ])
    assert(ret.toDID.isInstanceOf[DID])
    assertEquals(ret.subject, "123456789abcdefghijk")
    assertEquals(ret.query, "a=1")
  }

  test("DIDSubjectQ as DID") {
    val ret = DIDSubjectQ(didWithQuery)
    assert((ret: DID).isInstanceOf[DID])
    assertEquals(ret.scheme, "did")
    assertEquals(ret.namespace, "example")
    assertEquals(ret.specificId, "123456789abcdefghijk?a=1")
  }

  test("DIDSubjectQ parse") {
    val ret = s"\"$didWithQuery\"".fromJson[DIDSubjectQ]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj.did, didWithQuery)
    }
  }

  test("DIDSubjectQ parse (keep the empty Quest - keep '?' at the end)") {
    val ret = s"\"$didWithQueryEmpty\"".fromJson[DIDSubjectQ]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj.did, didWithQueryEmpty)
    }
  }

  test("DIDSubjectQ serialize & parse") {
    val ret = DIDSubjectQ(didWithQuery).toJson
    assertEquals(ret, s"\"$didWithQuery\"")
  }

  // MUST parse DIDSubjectQ
  Seq(
    """did:m:s?q""",
    """did:m:s""",
    """did:m:s?""",
    """did:m:s????""",
  ).foreach { did =>
    test(s"DIDSubjectQ apply parse '$did'") {
      val ret = DIDSubjectQ(did)
      assert(ret.isInstanceOf[DIDSubjectQ])
      assert(ret.toDID.isInstanceOf[DID])
    }
  }

  // MUST FAIL parsing DIDSubjectQ
  Seq(
    """did:m:s?q#f""",
    """did:m:s#""",
    """did:m:s#f""",
    """did:m:s/p?q#f""",
  ).foreach { invalidDID =>
    test(s"DIDSubjectQ apply MUST fail for '$invalidDID'") {
      interceptMessage[java.lang.AssertionError](s"Fail to parse DIDSubjectQ: '$invalidDID'") {
        DIDSubjectQ(invalidDID)
      }
    }
  }

  // ##################################
  // ###  DIDSubject & DIDSubjectQ  ###
  // ##################################
  {
    val ret_did = DIDSubject(did)
    val retQ_did = DIDSubjectQ(did)
    val retQ_didWithQuery = DIDSubjectQ(didWithQuery)
    val retQ_didWithQueryEmpty = DIDSubjectQ(didWithQueryEmpty)

    val ret_another = DIDSubject("did:example:another")
    val retQ_another = DIDSubjectQ("did:example:another")

    test("Compare DIDSubject and DIDSubjectQ - Equal") {
      assertEquals(ret_did, DIDSubject(did))
      assertEquals(retQ_did, DIDSubjectQ(did))
      assertNotEquals(retQ_did, retQ_didWithQuery)
      assertNotEquals(retQ_did, retQ_didWithQueryEmpty)
    }

    test("Compare DIDSubject and DIDSubjectQ - Equal (runtime is of type String)") {
      assert(ret_did == retQ_did)
      assert(ret_another == retQ_another)
      assert(ret_did != retQ_didWithQueryEmpty)
    }

    test("Compare DIDSubject and DIDSubjectQ - sameSubject") {
      assert(retQ_did.sameSubject(ret_did))
    }
    test("Compare DIDSubject and DIDSubjectQ - sameSubject (different subjects)") {
      assert(!ret_did.sameSubject(ret_another))
      assert(!retQ_did.sameSubject(ret_another))
      assert(!retQ_didWithQuery.sameSubject(ret_another))
      assert(!retQ_didWithQueryEmpty.sameSubject(ret_another))
    }

    test("Compare DIDSubject and DIDSubjectQ - sameSubjectQ") {
      assert(retQ_did.sameSubjectQ(retQ_did))
      assert(retQ_did.sameSubjectQ(retQ_didWithQuery))
      assert(retQ_did.sameSubjectQ(retQ_didWithQueryEmpty))
      assert(retQ_did.sameSubjectQ(retQ_did))
      assert(ret_did.sameSubjectQ(retQ_did))
      // assert(ret_did.sameSubjectQ(retQ_didWithQuery))
      assert(ret_did.sameSubjectQ(retQ_didWithQueryEmpty))
    }
    test("Compare DIDSubject and DIDSubjectQ - sameSubject (different subjects)") {
      assert(!ret_did.sameSubjectQ(retQ_another))
      assert(!retQ_did.sameSubjectQ(retQ_another))
      assert(!retQ_didWithQuery.sameSubjectQ(retQ_another))
      assert(!retQ_didWithQueryEmpty.sameSubjectQ(retQ_another))
    }

    val didSubjects = Seq(ret_did, ret_another)
    val didSubjectQs = Seq(retQ_did, retQ_didWithQuery, retQ_didWithQueryEmpty, retQ_another)
    didSubjects.foreach(did =>
      didSubjectQs.foreach { didQ =>
        test(
          s"Compare DIDSubject(Q) - commutative property: '$did' ${if (did.sameSubjectQ(didQ)) "==" else "!="} '$didQ'"
        ) {
          assertEquals(did.sameSubjectQ(didQ), didQ.sameSubject(did))
        }
      }
    )

  }

}
