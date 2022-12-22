package fmgp.did.comm

import munit._

import fmgp.did._
import fmgp.crypto._

/** didJVM/testOnly fmgp.did.comm.OpaqueTypesSuite
  *
  * didJS/testOnly fmgp.did.comm.OpaqueTypesSuite
  */
class OpaqueTypesSuite extends ZSuite {
  test("MsgId new random ID") {
    val id1 = MsgID()
    val id2 = MsgID()
    assertNotEquals(id1, id2)
    println(id1)
    println(id2)
  }
}
