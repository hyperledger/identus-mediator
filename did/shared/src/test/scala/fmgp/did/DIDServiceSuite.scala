package fmgp.did

import munit._
import zio._
import zio.json._
import zio.json.ast.{Json, JsonCursor}
import fmgp.did.DIDDocument

/** didJVM/testOnly fmgp.did.DIDServiceSuite */
class DIDServiceSuite extends FunSuite {

  test("Example 20 parse") {
    val json = DIDExamples.EX20.fromJson[ast.Json]
    val cursor = JsonCursor.field("service").isArray.element(0)
    val ret = json.flatMap(_.get(cursor)).flatMap(_.toJson.fromJson[DIDService])
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, DIDExamples.EX20_DIDService)
    }
  }

  test("DIDCommMessaging with URI on the serviceEndpoint") {
    val service =
      """{"id": "did:example:123#didcomm-1","type": "DIDCommMessaging",
        |"serviceEndpoint": "https://fmgp.app/"
        |}""".stripMargin
    val ret = service.fromJson[DIDService]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(
          obj,
          DIDServiceGeneric(
            id = "did:example:123#didcomm-1",
            `type` = "DIDCommMessaging",
            serviceEndpoint = Json.Str("https://fmgp.app/")
          )
        )

    }
  }

  test("DIDCommMessaging with an array of serviceEndpoint URI") {
    val service =
      """{"id": "did:example:123#didcomm-1","type": "DIDCommMessaging",
        |"serviceEndpoint": ["https://fmgp.app/", "https://did.fmgp.app/test"]
        |}""".stripMargin
    val ret = service.fromJson[DIDService]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(
          obj,
          DIDServiceGeneric(
            id = "did:example:123#didcomm-1",
            `type` = "DIDCommMessaging",
            serviceEndpoint = Json.Arr(
              Json.Str("https://fmgp.app/"),
              Json.Str("https://did.fmgp.app/test")
            )
          )
        )
    }
  }

  // TODO for DIDCommV2.1
  // test("DIDCommV2.1 (new)- DIDCommMessaging with a single serviceEndpoint") {
  //   val service =
  //     """{
  //       |  "id": "did:example:123456789abcdefghi#didcomm-1",
  //       |  "type": "DIDCommMessaging",
  //       |  "serviceEndpoint": {
  //       |    "uri": "https://example.com/path",
  //       |    "accept": ["didcomm/v2","didcomm/aip2;env=rfc587"],
  //       |    "routingKeys": ["did:example:somemediator#somekey"]
  //       |  }
  //       |}""".stripMargin
  //   val ret = service.fromJson[DIDService]
  //   ret match {
  //     case Left(error) => fail(error)
  //     case Right(obj)  => assertEquals(obj, DIDExamples.EX20_DIDService)
  //   }
  // }

  test("DIDCommMessaging with an array of serviceEndpoint objects") {
    val service =
      """{
        |  "id": "did:example:123456789abcdefghi#didcomm-1",
        |  "type": "DIDCommMessaging",
        |  "serviceEndpoint": [{
        |    "uri": "https://example.com/path",
        |    "accept": ["didcomm/v2","didcomm/aip2;env=rfc587"],
        |    "routingKeys": ["did:example:somemediator#somekey"]
        |  }]
        |}""".stripMargin
    val ret = service.fromJson[DIDService]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        val serviceExpected = DIDServiceGeneric(
          id = "did:example:123456789abcdefghi#didcomm-1",
          `type` = "DIDCommMessaging",
          serviceEndpoint = Json.Arr(
            Json.Obj(
              Chunk(
                ("uri", Json.Str("https://example.com/path")),
                ("accept", Json.Arr(Json.Str("didcomm/v2"), Json.Str("didcomm/aip2;env=rfc587"))),
                ("routingKeys", Json.Arr(Json.Str("did:example:somemediator#somekey")))
              )
            )
          )
        )
        assertEquals(obj, serviceExpected)
    }
  }
}
