package fmgp.did.comm

import munit._

import zio._
import zio.json._

/** didJVM/testOnly fmgp.did.comm.PlaintextMessageSuite */
class PlaintextMessageSuite extends FunSuite {

  test("parse plaintextMessage (into PlaintextMessageClass)") {
    val ret = EncryptedMessageExamples.plaintextMessage.fromJson[PlaintextMessageClass]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.`type`, PIURI("https://example.com/protocols/lets_do_lunch/1.0/proposal"))
    }
  }

  test("parse plaintextMessage") {
    val ret = EncryptedMessageExamples.plaintextMessage.fromJson[PlaintextMessage]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.`type`, PIURI("https://example.com/protocols/lets_do_lunch/1.0/proposal"))
    }
  }

  test("parse plaintextMessage with return_route") {
    val ret = """{
      |  "id":"1234567890",
      |  "type":"https://example.com/protocols/lets_do_lunch/1.0/proposal",
      |  "from":"did:example:alice",
      |  "to":["did:example:bob"],
      |  "return_route":"all",
      |  "body":{"messagespecificattribute":"and its value"}
      |}""".stripMargin.fromJson[PlaintextMessage]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        obj.return_route match
          case None        => fail("Missing the return_route")
          case Some(value) => assertEquals(value, ReturnRoute.all)
    }
  }
}
