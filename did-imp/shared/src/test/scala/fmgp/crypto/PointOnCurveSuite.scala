package fmgp.crypto

import munit._
import zio._
import zio.json._
import fmgp.util.Base64

/** didImpJVM/testOnly fmgp.crypto.PointOnCurveSuite
  *
  * @see
  *   https://8gwifi.org/jwkfunctions.jsp to generate keys for testing
  */
class PointOnCurveSuite extends ZSuite {
  val ex1_P_256 = """{
    "kid":"did:example:alice#key-2",
    "kty":"EC",
    "d":"7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A",
    "crv":"P-256",
    "x":"2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY",
    "y":"BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w"
  }"""

  test("Positive test for P_256") {
    val key = ex1_P_256.fromJson[ECPrivateKey].toOption.get
    val ret = PointOnCurve.isPointOnCurveP_256(
      Base64(key.x).decodeToBigInt,
      Base64(key.y).decodeToBigInt
    )
    assert(ret)
  }
}
