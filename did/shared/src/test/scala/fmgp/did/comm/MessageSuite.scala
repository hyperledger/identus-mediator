package fmgp.did.comm

import munit._

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.RawOperations._

import zio._
import zio.json._
import zio.json.ast.Json

class MessageSuite extends ZSuite {
  test("Parse PlaintextMessage") {
    EncryptedMessageExamples.plaintextMessage.fromJson[Message] match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assert(obj.isInstanceOf[PlaintextMessage])
        assert(!obj.isInstanceOf[SignedMessage])
        assert(!obj.isInstanceOf[EncryptedMessage])
    }
  }

  test("Parse SignedMessage") {
    SignedMessageExample.allSignedMessage_json.map { str =>
      str.fromJson[Message] match {
        case Left(error) => fail(error)
        case Right(obj) =>
          assert(!obj.isInstanceOf[PlaintextMessage])
          assert(obj.isInstanceOf[SignedMessage])
          assert(!obj.isInstanceOf[EncryptedMessage])
      }
    }
  }

  test("Parse EncryptedMessage") {
    EncryptedMessageExamples.allEncryptedMessage.map { str =>
      str.fromJson[Message] match {
        case Left(error) => fail(error)
        case Right(obj) =>
          assert(!obj.isInstanceOf[PlaintextMessage])
          assert(!obj.isInstanceOf[SignedMessage])
          assert(obj.isInstanceOf[EncryptedMessage])
      }
    }
  }

  test("Encode PlaintextMessage") {
    val str = EncryptedMessageExamples.plaintextMessage
    str.fromJson[Message] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj.toJson.fromJson[Json], str.fromJson[Json])
    }
  }

  test("Encode SignedMessage") {
    SignedMessageExample.allSignedMessage_json.map { str =>
      str.fromJson[Message] match {
        case Left(error) => fail(error)
        case Right(obj)  => assertEquals(obj.toJson.fromJson[Json], str.fromJson[Json])
      }
    }
  }

  test("Encode EncryptedMessage") {
    EncryptedMessageExamples.allEncryptedMessage.map { str =>
      str.fromJson[Message] match {
        case Left(error) => fail(error)
        case Right(obj)  => assertEquals(obj.toJson.fromJson[Json], str.fromJson[Json])
      }
    }
  }
}
