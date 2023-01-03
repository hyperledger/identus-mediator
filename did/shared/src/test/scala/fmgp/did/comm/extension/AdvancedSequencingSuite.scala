package fmgp.did.comm.extension

import munit._
import zio.json._

import fmgp.did._
import fmgp.did.comm._

/** didJVM/testOnly fmgp.did.comm.extension.AdvancedSequencingSuite
  */
class AdvancedSequencingSuite extends ZSuite {

  val ex_received_orders =
    """[{"id": "did:ex:bob", "last": 2, "gaps": []},{"id": "did:ex:alice", "last": 4, "gaps": [2,3]}]"""

  val expeted_received_orders_obj1 =
    ReceivedOrdersElement(DIDSubject("did:ex:bob"), SenderOrder(2), Seq())
  val expeted_received_orders_obj2 =
    ReceivedOrdersElement(DIDSubject("did:ex:alice"), SenderOrder(4), Seq(SenderOrder(2), SenderOrder(3)))

  test("Parse ReceivedOrdersElement") {
    ex_received_orders.fromJson[Seq[ReceivedOrdersElement]] match
      case Left(error) => fail(error)
      case Right(Seq(obj1, obj2)) =>
        assertEquals(obj1, expeted_received_orders_obj1)
        assertEquals(obj2, expeted_received_orders_obj2)
      case Right(obj) => fail("ReceivedOrdersElement obj is not as expeted")
  }
}
