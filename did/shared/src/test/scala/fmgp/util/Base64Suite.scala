package fmgp.util

import munit._

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
      assertEquals(Base64.encode(input).decode, input, "encode and decode")
      assertEquals(Base64.encode(input).urlBase64, expetedUrl, "to url")
      assertEquals(Base64.encode(input).basicBase64, expeted, "to base64")

      assertEquals(Base64.fromBase64url(expetedUrl).decode, input, "from url")
      assertEquals(Base64.fromBase64(expeted).decode, input, "from base64")
    }
  }

}
