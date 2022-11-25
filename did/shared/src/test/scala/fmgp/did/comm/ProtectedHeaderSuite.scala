package fmgp.did.comm

import munit._

import zio._
import zio.json._
import zio.json.ast.Json
import fmgp.util.Base64

/** didJVM/testOnly fmgp.did.comm.ProtectedHeaderSuite */

class ProtectedHeaderSuite extends ZSuite {

  test("parse & serialize ENCAlgorithm XC20P") {
    assertEquals(ENCAlgorithm.XC20P.toJson, """"XC20P"""")
    assertEquals(ENCAlgorithm.XC20P.toJson.fromJson[ENCAlgorithm], Right(ENCAlgorithm.XC20P))
  }
  test("parse & serialize ENCAlgorithm A256GCM") {
    assertEquals(ENCAlgorithm.A256GCM.toJson, """"A256GCM"""")
    assertEquals(ENCAlgorithm.A256GCM.toJson.fromJson[ENCAlgorithm], Right(ENCAlgorithm.A256GCM))
  }
  test("parse & serialize ENCAlgorithm A256CBC-HS512") {
    assertEquals(ENCAlgorithm.`A256CBC-HS512`.toJson, """"A256CBC-HS512"""")
    assertEquals(ENCAlgorithm.`A256CBC-HS512`.toJson.fromJson[ENCAlgorithm], Right(ENCAlgorithm.`A256CBC-HS512`))
  }
  test("parse & serialize KWAlgorithm ECDH-ES+A256KW") {
    assertEquals(KWAlgorithm.`ECDH-ES+A256KW`.toJson, """"ECDH-ES+A256KW"""")
    assertEquals(KWAlgorithm.`ECDH-ES+A256KW`.toJson.fromJson[KWAlgorithm], Right(KWAlgorithm.`ECDH-ES+A256KW`))
  }
  test("parse & serialize KWAlgorithm ECDH-1PU+A256KW") {
    assertEquals(KWAlgorithm.`ECDH-1PU+A256KW`.toJson, """"ECDH-1PU+A256KW"""")
    assertEquals(KWAlgorithm.`ECDH-1PU+A256KW`.toJson.fromJson[KWAlgorithm], Right(KWAlgorithm.`ECDH-1PU+A256KW`))
  }

//   val h1 =
//     "eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19"
  val ex1 =
    "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkpIanNtSVJaQWFCMHpSR193TlhMVjJyUGdnRjAwaGRIYlc1cmo4ZzBJMjQifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0"
  val ex2 =
    "eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTM4NCIsIngiOiIxNjFhZ0dlYWhHZW1IZ25qSG1RX0JfU09OeUJWZzhWTGRoVGdWNVc1NFZiYWJ5bGxpc3NuWjZXNzc5SW9VcUtyIiwieSI6ImNDZXFlRmdvYm9fY1ItWTRUc1pCWlg4dTNCa2l5TnMyYi12ZHFPcU9MeUNuVmdPMmpvN25zQV9JQzNhbnQ5T1gifSwiYXB2IjoiTEpBOUVva3M1dGFtVUZWQmFsTXdCaEo2RGtEY0o4SEs0U2xYWldxRHFubyIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC1FUytBMjU2S1cifQ"
  val ex3 =
    "eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTUyMSIsIngiOiJBRWtrc09abW1oZkZYdU90MHMybVdFYlVybVQ3OXc1SFRwUm9TLTZZNXpkYlk5T0I5b2RHb2hDYm1PeGpqY2VhWUU5ZnNaX3RaNmdpTGFBNUFEUnBrWE5VIiwieSI6IkFDaWJnLXZEMmFHVEpHbzlmRUl6Q1dXT2hSVUlObFg3Q1hGSTJqeDlKVDZmTzJfMGZ3SzM2WTctNHNUZTRpRVVSaHlnU1hQOW9TVFczTkdZTXVDMWlPQ3AifSwiYXB2IjoiR09lbzc2eW02TkNnOVdXTUVZZlcwZVZEVDU2Njh6RWhsMnVBSVctRS1IRSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiRUNESC1FUytBMjU2S1cifQ"
  val ex4 =
    "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkdGY01vcEpsamY0cExaZmNoNGFfR2hUTV9ZQWY2aU5JMWRXREd5VkNhdzAifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInNraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXkteDI1NTE5LTEiLCJhcHUiOiJaR2xrT21WNFlXMXdiR1U2WVd4cFkyVWphMlY1TFhneU5UVXhPUzB4IiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ"
  val ex5 =
    "eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTI1NiIsIngiOiJObHJ3UHZ0SUluZWNpeUVrYTRzMi00czhPalRidEZFQVhmTC12Z2x5enFvIiwieSI6ImhiMnZkWE5zSzVCQ2U3LVhaQ0dfLTY0R21UT19rNUlNWFBaQ00xdGFUQmcifSwiYXB2Ijoiei1McXB2VlhEYl9zR1luM21qUUxwdXUyQ1FMZXdZdVpvVFdPSVhQSDNGTSIsInNraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktcDI1Ni0xIiwiYXB1IjoiWkdsa09tVjRZVzF3YkdVNllXeHBZMlVqYTJWNUxYQXlOVFl0TVEiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLWVuY3J5cHRlZCtqc29uIiwiZW5jIjoiQTI1NkNCQy1IUzUxMiIsImFsZyI6IkVDREgtMVBVK0EyNTZLVyJ9"
  val ex6 =
    "eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTUyMSIsIngiOiJBYmxoeVVENUxYNE9zWDhGRTVaODRBX09CYThiOHdhVUhXSFExbTBnczhuSFVERDdySDlJRWRZbzJUSzFQYU5ha05aSk54a1FBWC1aUkxWa1BoNnV4eTJNIiwieSI6IkFQTjh6c0xEZGJpVjN0LTloWTJFQzFVZWEzTm5tMzFtNWowRmNiUWM0Y2ZWQmFNdzVCQ2VpcU9QWkljZTVMNjI4bnVORkxKR2szSjh6SVBPYUlLU0xmaTEifSwiYXB2IjoiR09lbzc2eW02TkNnOVdXTUVZZlcwZVZEVDU2Njh6RWhsMnVBSVctRS1IRSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0"

  test("Parse Header ex1") {
    Base64.fromBase64url(ex1).decode.fromJson[ProtectedHeader] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(Base64.encode(obj.toJson).urlBase64, ex1)
    }
  }
  test("Parse Header ex2") {
    Base64.fromBase64url(ex2).decode.fromJson[ProtectedHeader] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(Base64.encode(obj.toJson).urlBase64, ex2)
    }
  }
  test("Parse Header ex3") {
    Base64.fromBase64url(ex3).decode.fromJson[ProtectedHeader] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(Base64.encode(obj.toJson).urlBase64, ex3)
    }
  }
  test("Parse Header ex4") {
    Base64.fromBase64url(ex4).decode.fromJson[ProtectedHeader] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(Base64.encode(obj.toJson).urlBase64, ex4)
    }
  }
  test("Parse Header ex5") {
    Base64.fromBase64url(ex5).decode.fromJson[ProtectedHeader] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(Base64.encode(obj.toJson).urlBase64, ex5)
    }
  }
  test("Parse Header ex6") {
    Base64.fromBase64url(ex6).decode.fromJson[ProtectedHeader] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(Base64.encode(obj.toJson).urlBase64, ex6)
    }
  }
}
