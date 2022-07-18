package fmgp.crypto

import munit._
import zio.json._
import fmgp.did.comm.DIDCommExamples

class KeyStoreSuite extends FunSuite {

  test("parse KeyStore") {
    val ret = DIDCommExamples.recipientSecrets.fromJson[KeyStore]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj.keys.size, 9)
    }
  }

}
