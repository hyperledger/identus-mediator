package fmgp.did.comm.protocol.mediatorcoordination2

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._

/** didJVM/testOnly fmgp.did.comm.protocol.mediatorcoordination2.MediatorCoordinationSuite
  */
class MediatorCoordinationSuite extends FunSuite {

  val msg = """{
    |  "id" : "3687298f-7830-4e43-87e2-ea67c1ae7f85",
    |  "type" : "https://didcomm.org/coordinate-mediation/2.0/keylist-update",
    |  "to" : [
    |    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
    |  ],
    |  "from" : "did:peer:2.Ez6LSqZd7Zrp9LcHBEiyMLvKVUxFYb3hNWRxq8cCXbfx4zs6u.Vz6MksTvq16LvtvSqTg8ru7XEXf65Um8WKP7Zm3Vrv4incAhb.SW10",
    |  "created_time" : 1683026354666,
    |  "body" : {
    |    "updates" : [
    |      {
    |        "action" : "add",
    |        "recipient_did" : "did:peer:2.Ez6LSrLYsJ7z4hUuDwLPLoKUJZPt64nv6oqQSPj4AY9TMWPMk.Vz6MkkaimikhRgvoots6ei1d7aq6xS4EZT6gd1UgRQD4uby4W.SeyJyIjpbXSwicyI6ImRpZDpwZWVyOjIuRXo2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eS5WejZNa2hoMWU1Q0VZWXE2SkJVY1RaNkNwMnJhbkNXUnJ2N1lheDNMZTRONTlSNmRkLlNleUowSWpvaVpHMGlMQ0p6SWpvaWFIUjBjSE02THk5aGJHbGpaUzVrYVdRdVptMW5jQzVoY0hBdklpd2ljaUk2VzEwc0ltRWlPbHNpWkdsa1kyOXRiUzkyTWlKZGZRIiwiYSI6W10sInQiOiJkbSJ9"
    |      }
    |    ]
    |  },
    |  "attachments" : []
    |}""".stripMargin

  test("Parse a keylist-update example") {

    val fMsg = msg
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toKeylistUpdate

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "3687298f-7830-4e43-87e2-ea67c1ae7f85")
        msg.updates match
          case Seq((fromto, keylistAction)) =>
            assertEquals(keylistAction, KeylistAction.add)
            assertEquals(
              fromto,
              FROMTO(
                "did:peer:2.Ez6LSrLYsJ7z4hUuDwLPLoKUJZPt64nv6oqQSPj4AY9TMWPMk.Vz6MkkaimikhRgvoots6ei1d7aq6xS4EZT6gd1UgRQD4uby4W.SeyJyIjpbXSwicyI6ImRpZDpwZWVyOjIuRXo2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eS5WejZNa2hoMWU1Q0VZWXE2SkJVY1RaNkNwMnJhbkNXUnJ2N1lheDNMZTRONTlSNmRkLlNleUowSWpvaVpHMGlMQ0p6SWpvaWFIUjBjSE02THk5aGJHbGpaUzVrYVdRdVptMW5jQzVoY0hBdklpd2ljaUk2VzEwc0ltRWlPbHNpWkdsa1kyOXRiUzkyTWlKZGZRIiwiYSI6W10sInQiOiJkbSJ9"
              )
            )
          case _ => fail("body.updates must have one element")

      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }

  }
}
