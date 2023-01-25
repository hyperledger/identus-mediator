package fmgp.did.comm

import munit._
import zio.json._
import scala.util.chaining._

import fmgp.did._
import java.net.{URI, URL}

/** didJVM/testOnly fmgp.did.comm.OutOfBandSuite */
class OutOfBandSuite extends FunSuite {

  val oobMessage = """{
    |"type":"https://didcomm.org/out-of-band/2.0/invitation",
    |"id":"599f3638-b563-4937-9487-dfe55099d900",
    |"from":"did:example:verifier",
    |"body":{"goal_code":"streamlined-vp","accept":["didcomm/v2"]}
    |}""".stripMargin
  val oobMessageBase64 =
    "eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOnZlcmlmaWVyIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ=="
  val oobURI = s"""https://example.com/some/path?_oob=$oobMessageBase64"""

  test(s"OutOfBand invitation as PlaintextMessage") {
    assert(OutOfBand.oob(oobURI).isDefined)
    val ret1 = OutOfBand.oobPlaintext(oobURI)
    val ret2 = OutOfBand.from(oobURI)
    val expeted = oobMessage.fromJson[PlaintextMessage].tap(o => assert(o.isRight)).getOrElse(fail("Must be Right"))

    assert(ret1.isRight)
    assertEquals(ret1, ret2)
    ret2 match
      case Left(value)                          => fail("Must be Right")
      case Right(OOB(data))                     => fail("Must be a OutOfBandPlaintext")
      case Right(OutOfBandPlaintext(msg, data)) => assertEquals(msg, expeted)
      case Right(OutOfBandSigned(msg, data))    => fail("Must be a OutOfBandPlaintext")
  }

  val tmp = "e30=" // oobMessageBase64
  Seq(
    (s"""?_oob=$tmp""", true), // +- undefined behavior => because is not really a complete url
    (s"""https://d?_oob=$tmp""", true),
    (s"""https://d?_oob=$tmp&none""", true),
    (s"""https://d?_oob=$tmp&_oob=""", true),
    (s"""https://d/path?_oob=$tmp""", true),
    (s"""https://d/path?oob=$tmp""", false), // missing '_' from _oob
    ("""https://d/path?_oob=""", false), // missing data for _oob
    (s"""https://d/path?_oob=$tmp#""", true),
    (s"""https://d/path?_oob=$tmp#f""", true),
    (s"""https://d/path?_oob=$tmp#""", true),
  ).foreach {
    case (uri, true) =>
      test(s"OutOfBand oob will parse '$uri'") {
        // assert(OutOfBand.oob(URI(uri)).isDefined)
        // // Try(URL(uri)).map(url => assert(OutOfBand2.oob(url).isDefined))
        // if (!uri.startsWith("?")) assert(OutOfBand.oob(URL(uri)).isDefined)
        assert(OutOfBand.oob(uri).isDefined)
      }
    case (uri, false) =>
      test(s"OutOfBand oob will NOT parse '$uri'") {
        // assert(OutOfBand.oob(URI(uri)).isEmpty)
        // assert(OutOfBand.oob(URL(uri)).isEmpty)
        assert(OutOfBand.oob(uri).isEmpty)
      }
  }

}
