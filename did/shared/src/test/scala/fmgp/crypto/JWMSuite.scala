package fmgp.crypto

import fmgp.did.DIDDocument
import fmgp.did.comm.DIDCommExamples
import munit._
import zio.json._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import concurrent.ExecutionContext.Implicits.global

class JWMSuite extends FunSuite {

  test("sign and verify plaintextMessage using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    key
      .sign(DIDCommExamples.plaintextMessageObj)
      .flatMap(jwsObject => key.verify(jwsObject))
      .map(e => assert(e))
  }

  test("verify plaintextMessage example using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    key
      .verify(JWMExample.exampleSignatureES256K_obj)
      .map(e => assert(e))
  }

  test("sign and verify plaintextMessage using ECKey P-256") {
    val key: ECPrivateKey = JWKExamples.senderKeyP256.fromJson[ECPrivateKey].toOption.get
    key
      .sign(DIDCommExamples.plaintextMessageObj)
      .flatMap(jwsObject => key.verify(jwsObject))
      .map(e => assert(e))

  }

  test("verify plaintextMessage example using ECKey P-256") {
    val key: ECPrivateKey = JWKExamples.senderKeyP256.fromJson[ECPrivateKey].toOption.get
    key
      .verify(JWMExample.exampleSignatureES256_obj)
      .map(e => assert(e))
  }

  // https://github.com/scalameta/munit/issues/554
  test("sign and verify plaintextMessage using ECKey Ed25519") {
    val key = JWKExamples.senderKeyEd25519.fromJson[PrivateKey].toOption.get
    key
      .sign(DIDCommExamples.plaintextMessageObj)
      .flatMap(jwsObject => key.verify(jwsObject))
      .map(e => assert(e))
  }

  test("verify plaintextMessage example using ECKey Ed25519") {
    val key = JWKExamples.senderKeyEd25519.fromJson[PrivateKey].toOption.get
    key
      .verify(JWMExample.exampleSignatureEdDSA_obj)
      .map(e => assert(e))
  }

  // FAIL
  test("fail verify plaintextMessage using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    key
      .verify(JWMExample.exampleSignatureEdDSA_failSignature_obj)
      .map(e => assert(!e))
  }
}
