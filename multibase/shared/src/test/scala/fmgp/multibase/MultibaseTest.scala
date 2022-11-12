package fmgp.multibase

import munit._

class MultibaseTest extends ZSuite {

  test("contains correct code and name mappings for the Base.") {
    Base.Codes.foreach { case (code, base) =>
      assertEquals(base.code, code)
    }

    Base.Names.foreach { case (name, base) =>
      assertEquals(base.name, name)
    }
  }

  test("convert given string to correct encodings.") {
    val str = "Multibase is awesome! \\o/"

    for (base <- Base.Codes.values if base != Base.Base1) {
      assertEquals(Multibase.decodeToString(Multibase.encodeString(base, str)), str)
    }

    interceptMessage[UnsupportedOperationException]("Base1 is not supported yet!") {
      Multibase.decodeToString(Multibase.encodeString(Base.Base1, str))
    }

    assertEquals(Base.Unsupported.size, 1)
    assertEquals(Base.Unsupported(Base.Base1.code), Base.Base1)

    val illegalDate = "abc"
    interceptMessage[IllegalArgumentException]("Cannot get Multibase type from input data: " + illegalDate) {
      Multibase.decode(illegalDate)
    }

    val strs =
      Seq("zQmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB", "zQmatmE9msSfkKxoffpHwNLNKgwZG8eT9Bud6YoPab52vpy", "z11")

    for (
      s <- strs;
      base <- Base.Codes.values if base != Base.Base1
    ) assertEquals(Multibase.decodeToString(Multibase.encodeString(base, s)), s)

  }

  case class TestCase(name: String, str: String, encoded: String)

  /** Test cases from: https://github.com/multiformats/js-multibase/blob/master/test/multibase.spec.js
    */
  val testCases = Seq(
    TestCase("base2", "yes mani !", "01111001011001010111001100100000011011010110000101101110011010010010000000100001"),
    TestCase("base8", "yes mani !", "7171312714403326055632220041"),
    TestCase("base10", "yes mani !", "9573277761329450583662625"),
    TestCase("base16", "yes mani !", "f796573206d616e692021"),

//    TestCase("base16", 0x01.toString, "f01"),
//    TestCase("base16", 0x0f.toString, "f0f"),
    TestCase("base16", "f", "f66"),
    TestCase("base16", "fo", "f666f"),
    TestCase("base16", "foo", "f666f6f"),
    TestCase("base16", "foob", "f666f6f62"),
    TestCase("base16", "fooba", "f666f6f6261"),
    TestCase("base16", "foobar", "f666f6f626172"),
    TestCase("base32", "yes mani !", "bpfsxgidnmfxgsibb"),
    TestCase("base32", "f", "bmy"),
    TestCase("base32", "fo", "bmzxq"),
    TestCase("base32", "foo", "bmzxw6"),
    TestCase("base32", "foob", "bmzxw6yq"),
    TestCase("base32", "fooba", "bmzxw6ytb"),
    TestCase("base32", "foobar", "bmzxw6ytboi"),
    TestCase("base32upper", "yes mani !", "bpfsxgidnmfxgsibb".toUpperCase),
    TestCase("base32upper", "f", "bmy".toUpperCase),
    TestCase("base32upper", "fo", "bmzxq".toUpperCase),
    TestCase("base32upper", "foo", "bmzxw6".toUpperCase),
    TestCase("base32upper", "foob", "bmzxw6yq".toUpperCase),
    TestCase("base32upper", "fooba", "bmzxw6ytb".toUpperCase),
    TestCase("base32upper", "foobar", "bmzxw6ytboi".toUpperCase),
    TestCase("base32pad", "yes mani !", "cpfsxgidnmfxgsibb"),
    TestCase("base32pad", "f", "cmy======"),
    TestCase("base32pad", "fo", "cmzxq===="),
    TestCase("base32pad", "foo", "cmzxw6==="),
    TestCase("base32pad", "foob", "cmzxw6yq="),
    TestCase("base32pad", "fooba", "cmzxw6ytb"),
    TestCase("base32pad", "foobar", "cmzxw6ytboi======"),
    TestCase("base32padupper", "yes mani !", "cpfsxgidnmfxgsibb".toUpperCase),
    TestCase("base32padupper", "f", "cmy======".toUpperCase),
    TestCase("base32padupper", "fo", "cmzxq====".toUpperCase),
    TestCase("base32padupper", "foo", "cmzxw6===".toUpperCase),
    TestCase("base32padupper", "foob", "cmzxw6yq=".toUpperCase),
    TestCase("base32padupper", "fooba", "cmzxw6ytb".toUpperCase),
    TestCase("base32padupper", "foobar", "cmzxw6ytboi======".toUpperCase),
    TestCase("base32hex", "yes mani !", "vf5in683dc5n6i811"),
    TestCase("base32hex", "f", "vco"),
    TestCase("base32hex", "fo", "vcpng"),
    TestCase("base32hex", "foo", "vcpnmu"),
    TestCase("base32hex", "foob", "vcpnmuog"),
    TestCase("base32hex", "fooba", "vcpnmuoj1"),
    TestCase("base32hex", "foobar", "vcpnmuoj1e8"),
    TestCase("base32hexupper", "yes mani !", "vf5in683dc5n6i811".toUpperCase),
    TestCase("base32hexupper", "f", "vco".toUpperCase),
    TestCase("base32hexupper", "fo", "vcpng".toUpperCase),
    TestCase("base32hexupper", "foo", "vcpnmu".toUpperCase),
    TestCase("base32hexupper", "foob", "vcpnmuog".toUpperCase),
    TestCase("base32hexupper", "fooba", "vcpnmuoj1".toUpperCase),
    TestCase("base32hexupper", "foobar", "vcpnmuoj1e8".toUpperCase),
    TestCase("base32hexpad", "yes mani !", "tf5in683dc5n6i811"),
    TestCase("base32hexpad", "f", "tco======"),
    TestCase("base32hexpad", "fo", "tcpng===="),
    TestCase("base32hexpad", "foo", "tcpnmu==="),
    TestCase("base32hexpad", "foob", "tcpnmuog="),
    TestCase("base32hexpad", "fooba", "tcpnmuoj1"),
    TestCase("base32hexpad", "foobar", "tcpnmuoj1e8======"),
    TestCase("base32hexpadupper", "yes mani !", "tf5in683dc5n6i811".toUpperCase),
    TestCase("base32hexpadupper", "f", "tco======".toUpperCase),
    TestCase("base32hexpadupper", "fo", "tcpng====".toUpperCase),
    TestCase("base32hexpadupper", "foo", "tcpnmu===".toUpperCase),
    TestCase("base32hexpadupper", "foob", "tcpnmuog=".toUpperCase),
    TestCase("base32hexpadupper", "fooba", "tcpnmuoj1".toUpperCase),
    TestCase("base32hexpadupper", "foobar", "tcpnmuoj1e8======".toUpperCase),
    TestCase("base32z", "yes mani !", "hxf1zgedpcfzg1ebb"),
    TestCase("base58flickr", "yes mani !", "Z7Pznk19XTTzBtx"),
    TestCase("base58btc", "yes mani !", "z7paNL19xttacUY"),
    TestCase("base64", "÷ïÿ", "mw7fDr8O/"),
    TestCase("base64", "f", "mZg"),
    TestCase("base64", "fo", "mZm8"),
    TestCase("base64", "foo", "mZm9v"),
    TestCase("base64", "foob", "mZm9vYg"),
    TestCase("base64", "fooba", "mZm9vYmE"),
    TestCase("base64", "foobar", "mZm9vYmFy"),
    TestCase("base64pad", "f", "MZg=="),
    TestCase("base64pad", "fo", "MZm8="),
    TestCase("base64pad", "foo", "MZm9v"),
    TestCase("base64pad", "foob", "MZm9vYg=="),
    TestCase("base64pad", "fooba", "MZm9vYmE="),
    TestCase("base64pad", "foobar", "MZm9vYmFy"),
    TestCase("base64url", "÷ïÿ", "uw7fDr8O_"),
    TestCase("base64urlpad", "f", "UZg=="),
    TestCase("base64urlpad", "fo", "UZm8="),
    TestCase("base64urlpad", "foo", "UZm9v"),
    TestCase("base64urlpad", "foob", "UZm9vYg=="),
    TestCase("base64urlpad", "fooba", "UZm9vYmE="),
    TestCase("base64urlpad", "foobar", "UZm9vYmFy")
  )

  test("convert given string to correct encodings for all TestCases.") {
    for (TestCase(name, str, expEncoded) <- testCases) {
      val base = Base.Names(name)
      val encoded = Multibase.encodeString(base, str)
      assertEquals(encoded, expEncoded)
      assertEquals(Multibase.decodeToString(expEncoded), str)
    }
  }

}
