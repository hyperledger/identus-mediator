package fmgp.did.comm

import fmgp.did.DIDDocument
import fmgp.crypto._

// import fmgp.crypto.RawOperations._
import munit._
import zio.json._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import concurrent.ExecutionContext.Implicits.global

class SignedMessageSuite_Parse extends FunSuite {

  // ### parse ###

  test("parse SignedMessage") {
    val str = SignedMessageExample.exampleSignatureEdDSA_json.fromJson[SignedMessage] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, SignedMessageExample.exampleSignatureEdDSA_obj)
    }
  }
}
