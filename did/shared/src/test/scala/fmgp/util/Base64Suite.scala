package fmgp.util

import munit._
import zio.json._

// didJVM/testOnly fmgp.util.Base64Suite
class Base64Suite extends FunSuite {

  val examples = Seq(
    ("", "", ""),
    ("f", "Zg==", "Zg"),
    ("fo", "Zm8=", "Zm8"),
    ("foo", "Zm9v", "Zm9v"),
    ("foob", "Zm9vYg==", "Zm9vYg"),
    ("fooba", "Zm9vYmE=", "Zm9vYmE"),
    ("foobar", "Zm9vYmFy", "Zm9vYmFy"),
    ("""f{}">?L`{+""", "Znt9Ij4/TGB7Kw==", "Znt9Ij4_TGB7Kw")
  )

  examples.foreach { case (input, expeted, expetedUrl) =>
    test("Check Base64 '" + input + "'") {
      assertEquals(Base64.encode(input).decodeToString, input, "encode and decode")
      assertEquals(Base64.encode(input).urlBase64, expetedUrl, "to url")
      assertEquals(Base64.encode(input).basicBase64, expeted, "to base64")

      assertEquals(Base64.fromBase64url(expetedUrl).decodeToString, input, "from url")
      assertEquals(Base64.fromBase64(expeted).decodeToString, input, "from base64")
    }
  }

  case class A(b: B)
  object A {
    given decoder: JsonDecoder[A] = DeriveJsonDecoder.gen[A]
    given encoder: JsonEncoder[A] = DeriveJsonEncoder.gen[A]
  }
  case class B(x: String, y: Int)
  object B {
    given decoder: JsonDecoder[B] = DeriveJsonDecoder.gen[B]
    given encoder: JsonEncoder[B] = DeriveJsonEncoder.gen[B]
  }

  test("Check Base64Obj") {
    val obj = Base64Obj(A(B("xx", 123)))
    val expected = """"eyJiIjp7IngiOiJ4eCIsInkiOjEyM319"""" // {"b":{"x":"xx","y":123}}
    assertEquals(obj.toJson, expected)

    expected.fromJson[Base64Obj[A]] match {
      case Left(error)  => fail(error)
      case Right(value) => assertEquals(value, obj)
    }
  }
}
