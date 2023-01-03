package fmgp.did.comm.extension

import munit._
import zio.json._

import fmgp.did.comm._

/** didJVM/testOnly fmgp.did.comm.extension.L10nSuite
  */
class L10nSuite extends ZSuite {

  val ex1 = """{
    |  "id": "388d599a-fdc1-4890-b32a-be6cd3893564",
    |  "type": "https://didcomm.org/chess/1.0/move",
    |  "lang": "en",
    |  "l10n": {
    |    "inline": [
    |      {"fr", "comment", "C'est échec et mat, mon pote."}
    |      {"es", "comment", "Eso es jaque mate, amigo"}
    |    ]
    |  },
    |  "body": {
    |    "move": "BC4+",
    |    "comment": "That's checkmate, buddy."
    |  }
    |}""".stripMargin

  val expeted = Seq(
    L10nInline(lang = "fr", field = "comment", translation = "C'est échec et mat, mon pote."),
    L10nInline(lang = "es", field = "comment", translation = "Eso es jaque mate, amigo"),
  )

  test("Parse PlaintextMessage with field l10n") {
    ex1.fromJson[PlaintextMessage] match
      case Left(error) => fail(error)
      case Right(obj) =>
        obj.l10n match
          case None => fail("Message MUST have the field 'l10n'")
          case Some(value) =>
            value match
              case L10n(Some(inline), None, None) => assertEquals(`inline`, expeted)
              case _                              => fail("L10n obj is not as expeted")
  }

}
