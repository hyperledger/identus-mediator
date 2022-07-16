package fmgp.crypto

import fmgp.did.DIDDocument
import fmgp.did.comm.DIDCommExamples
import munit._
import zio.json._

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.scalajs.js.JavaScriptException
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import concurrent.ExecutionContext.Implicits.global

class JWMSuiteJS extends FunSuite {

  import scala.scalajs.js

  test("sign and verify an example") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    key.sign(DIDCommExamples.plaintextMessageObj).flatMap { jwsObject =>
      Future.sequence(
        Seq(
          key.verify(jwsObject).map(e => assert(e)),
          key.verify(JWMExample.exampleSignatureES256K_obj).map(e => assert(e))
        )
      )
    }
  }

  // TODO REMOVE or remake
  // test("sign and verify plaintextMessage") {
  //   val key = JWKECKey
  //     .apply(
  //       ECCurve.secp256k1,
  //       "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
  //       "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
  //     )
  //     .setKid("did:example:alice#key-3")
  //     .setD("N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA")

  //   val data = js.Dynamic.literal("urn:example:claim" -> "foo")
  //   val jwsObject = JWT.sign(data, key)

  //   val publicKey: JWKECKey = key // TODO .toPublicJWK()
  //   assert(Try { JWT.verify(jwsObject, publicKey) } match {
  //     case Success(i)                                              => true
  //     case Failure(JavaScriptException(ex: JWSVerificationFailed)) => false
  //     case Failure(ex)                                             => throw ex
  //   })
  //   assert(Try { JWT.verify(JWMExample.exampleSignatureES256K_obj.base64, publicKey) } match {
  //     case Success(i)                                              => true
  //     case Failure(JavaScriptException(ex: JWSVerificationFailed)) => false
  //     case Failure(ex)                                             => throw ex
  //   })
  // }

  // TODO REMOVE
  // test("sign and verify plaintextMessage using JWKCruve") {
  //   val jwk: JWKCruve = JWKExamples.senderSecp256k1.fromJson[JWKCruve].toOption.get
  //   val jwsObject = jwk.sign(DIDCommExamples.plaintextMessageObj)
  //   assert(jwk.verify(jwsObject))
  //   assert(jwk.verify(JWMExample.example))
  // }

}
