package fmgp.did.comm

import munit._
import zio.json._
import zio.json.ast.Json

/** didJVM/testOnly fmgp.did.comm.AttachmentSuite
  */
class AttachmentSuite extends FunSuite {

  val attachmentBase64 = """{
    |  "id": "1",
    |  "description": "example b64 encoded attachment",
    |  "data": {
    |    "base64": "WW91ciBob3ZlcmNyYWZ0IGlzIGZ1bGwgb2YgZWVscw=="
    |  }
    |}""".stripMargin

  val attachmentLink = """{
    |  "id": "2",
    |  "description": "example linked attachment",
    |  "data": {
    |    "hash": "<multi-hash>",
    |    "links": [
    |      "https://path/to/resource"
    |    ]
    |  }
    |}""".stripMargin

  val attachmentJson = """{
    |  "id": "x",
    |  "description": "example encrypted DIDComm message as attachment",
    |  "media_type": "application/didcomm-encrypted+json",
    |  "data": {
    |    "json": {
    |       "a": [1,2,3],
    |       "b": null,
    |       "c": {}
    |    }
    |  }
    |}""".stripMargin
  // |      //jwe json structure

  test("Example parse attachmentBase64") {
    val ret = attachmentBase64.fromJson[Attachment]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.id, Some("1"))
        assertEquals(obj.description, Some("example b64 encoded attachment"))
        obj.data match
          case AttachmentDataBase64(base64) =>
            assertEquals(base64.basicBase64, "WW91ciBob3ZlcmNyYWZ0IGlzIGZ1bGwgb2YgZWVscw==")
            assertEquals(base64.decodeToString, "Your hovercraft is full of eels")
          case _ => fail("Wrong AttachmentData type")
    }
  }

  test("Example parse attachmentLink") {
    val ret = attachmentLink.fromJson[Attachment]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.id, Some("2"))
        assertEquals(obj.description, Some("example linked attachment"))
        obj.data match
          case AttachmentDataLinks(links, hash) =>
            assertEquals(links, Seq("https://path/to/resource"))
            assertEquals(hash, "<multi-hash>")
          case _ => fail("Wrong AttachmentData type")
    }
  }

  test("Example parse attachmentJson") {
    val ret = attachmentJson.fromJson[Attachment]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.id, Some("x"))
        assertEquals(obj.description, Some("example encrypted DIDComm message as attachment"))
        assertEquals(obj.media_type, Some("application/didcomm-encrypted+json"))
        obj.data match
          case AttachmentDataJson(json) =>
            assertEquals(
              json,
              Json.Obj(
                "a" -> Json.Arr(Json.Num(1), Json.Num(2), Json.Num(3)),
                "b" -> Json.Null,
                "c" -> Json.Obj()
              )
            )
          case _ => fail("Wrong AttachmentData type")
    }
  }

  test("Check class Type when parsing an attachment with nulls on the data") {
    val attachment = """{"data": {"json": null, "links": null}}"""
    val ret = attachment.fromJson[Attachment]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        obj.data match
          case AttachmentDataJWS(jws, links)                     => fail("Type is ok but the behavior changed")
          case AttachmentDataLinks(links, hash)                  => fail("Type is ok but the behavior changed")
          case AttachmentDataBase64(base64)                      => fail("Type is ok but the behavior changed")
          case AttachmentDataJson(json)                          => assertEquals(json, Json.Null)
          case AttachmentDataAny(jws, hash, links, base64, json) => fail("Type is ok but the behavior changed")
    }
  }

  test("Check class Type when parsing an attachment with nulls on the data".ignore) {
    val attachment = """{"data": {"json": {"a":1}, "links": ["a","b"]}}"""
    val ret = attachment.fromJson[Attachment]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        obj.data match
          case AttachmentDataJWS(jws, links)    => fail("Wrong AttachmentData type")
          case AttachmentDataLinks(links, hash) => fail("Wrong AttachmentData type")
          case AttachmentDataBase64(base64)     => fail("Wrong AttachmentData type")
          case AttachmentDataJson(json)         => fail("Wrong AttachmentData type")
          case AttachmentDataAny(jws, hash, links, base64, json) =>
            assertEquals(json, Some(Json.Obj("a" -> Json.Num(1))))
            assertEquals(links, Some(Seq("a", "b")))
    }
  }

  /** If the do `new java.math.BigDecimal("1e214748364").toBigInteger`.
    *
    * The JVM will happily attempt to reserve GBs of heap for the conversion.
    */
  test("DOS attack: Death by a Thousand Zeros") {
    val attachment = """{"data": {"json": 1e214748364}}"""
    val ret = attachment.fromJson[Attachment]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        obj.data match
          case AttachmentDataJson(json) =>
            assertEquals(
              json,
              Json.Num(BigDecimal("1e214748364"))
            )
          case _ => fail("Wrong AttachmentData type")
    }
  }

}
