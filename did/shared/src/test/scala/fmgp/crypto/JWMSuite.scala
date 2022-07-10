package fmgp.crypto

import fmgp.did.DIDDocument
import fmgp.did.comm.DIDCommExamples
import munit._
import zio.json._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

class JWMSuite extends FunSuite {

  test("sign and verify plaintextMessage using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    val jwsObject = key.sign(DIDCommExamples.plaintextMessageObj)
    assert(key.verify(jwsObject))
    assert(key.verify(JWMExample.exampleSignatureES256K_obj))
  }

  test("verify plaintextMessage example using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    assert(key.verify(JWMExample.exampleSignatureES256K_obj))
  }

  test("sign and verify plaintextMessage using ECKey P-256") {
    val key: ECPrivateKey = JWKExamples.senderKeyP256.fromJson[ECPrivateKey].toOption.get
    val jwsObject = key.sign(DIDCommExamples.plaintextMessageObj)
    assert(key.verify(jwsObject))
    assert(key.verify(JWMExample.exampleSignatureES256_obj))
  }

  test("verify plaintextMessage example using ECKey P-256") {
    val key: ECPrivateKey = JWKExamples.senderKeyP256.fromJson[ECPrivateKey].toOption.get
    assert(key.verify(JWMExample.exampleSignatureES256_obj))
  }

  // https://github.com/scalameta/munit/issues/554
  test("sign and verify plaintextMessage using ECKey Ed25519") {
    val key = JWKExamples.senderKeyEd25519.fromJson[PrivateKey].toOption.get
    val jwsObject = key.sign(DIDCommExamples.plaintextMessageObj)
    assert(key.verify(jwsObject))
    assert(key.verify(JWMExample.exampleSignatureEdDSA_obj))
  }
  test("verify plaintextMessage example using ECKey Ed25519") {
    val key = JWKExamples.senderKeyEd25519.fromJson[PrivateKey].toOption.get
    assert(key.verify(JWMExample.exampleSignatureEdDSA_obj))
  }
}
