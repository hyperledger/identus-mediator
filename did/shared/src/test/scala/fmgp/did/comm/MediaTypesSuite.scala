package fmgp.did.comm

import munit._

import zio._
import zio.json._
import zio.json.ast.Json

/** didJVM/testOnly fmgp.did.comm.MediaTypesSuite */
import fmgp.did.comm.MediaTypes._
class MediaTypesSuite extends ZSuite {

  test("Encode MediaTypes") {
    assertEquals(PLAINTEXT.toJson, "\"application/didcomm-plain+json\"")
    assertEquals(SIGNED.toJson, "\"application/didcomm-signed+json\"")
    assertEquals(ENCRYPTED.toJson, "\"application/didcomm-encrypted+json\"")
    assertEquals(ANONCRYPT.toJson, "\"application/didcomm-encrypted+json\"")
    assertEquals(AUTHCRYPT.toJson, "\"application/didcomm-encrypted+json\"")
    assertEquals(ANONCRYPT_SIGN.toJson, "\"application/didcomm-encrypted+json\"")
    assertEquals(AUTHCRYPT_SIGN.toJson, "\"application/didcomm-encrypted+json\"")
    assertEquals(ANONCRYPT_AUTHCRYPT.toJson, "\"application/didcomm-encrypted+json\"")
  }

  test("Parse MediaTypes") {

    "\"application/didcomm-plain+json\"".fromJson[MediaTypes] match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj, PLAINTEXT)
        assertNotEquals(obj, SIGNED)
        assertNotEquals(obj, ENCRYPTED)
        assertNotEquals(obj, ANONCRYPT)
        assertNotEquals(obj, AUTHCRYPT)
        assertNotEquals(obj, ANONCRYPT_SIGN)
        assertNotEquals(obj, AUTHCRYPT_SIGN)
        assertNotEquals(obj, ANONCRYPT_AUTHCRYPT)
    }
    "\"application/didcomm-signed+json\"".fromJson[MediaTypes] match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertNotEquals(obj, PLAINTEXT)
        assertEquals(obj, SIGNED)
        assertNotEquals(obj, ENCRYPTED)
        assertNotEquals(obj, ANONCRYPT)
        assertNotEquals(obj, AUTHCRYPT)
        assertNotEquals(obj, ANONCRYPT_SIGN)
        assertNotEquals(obj, AUTHCRYPT_SIGN)
        assertNotEquals(obj, ANONCRYPT_AUTHCRYPT)
    }
    "\"application/didcomm-encrypted+json\"".fromJson[MediaTypes] match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertNotEquals(obj, PLAINTEXT)
        assertNotEquals(obj, SIGNED)
        assertEquals(obj, ENCRYPTED)
      // assertEquals(obj, ANONCRYPT)
      // assertEquals(obj, AUTHCRYPT)
      // assertEquals(obj, ANONCRYPT_SIGN)
      // assertEquals(obj, AUTHCRYPT_SIGN)
      // assertEquals(obj, ANONCRYPT_AUTHCRYPT)
    }

  }
}
