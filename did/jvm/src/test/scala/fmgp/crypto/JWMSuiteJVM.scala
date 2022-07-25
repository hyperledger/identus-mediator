package fmgp.crypto

import munit._
import zio.json._
import fmgp.did.DIDDocument
import fmgp.did.comm._

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.util.Base64URL

class JWMSuiteJVM extends FunSuite {

  test("sign and verify plaintextMessage") {
    val ecJWK: ECKey = ECKey // TODO use senderSecp256k1 parsed with
      .Builder(
        Curve.SECP256K1,
        Base64URL("aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk"),
        Base64URL("JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk")
      )
      .keyID("did:example:alice#key-3")
      .d(Base64URL("N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA"))
      .build()

    val jwsObject = ecJWK.sign(DIDCommExamples.plaintextMessageObj, JWAAlgorithm.ES256K)

    val ecPublicJWK: ECKey = ecJWK.toPublicJWK()
    assert(ecPublicJWK.verify(jwsObject, JWAAlgorithm.ES256K))
    assert(ecPublicJWK.verify(SignedMessageExample.exampleSignatureES256K_obj, JWAAlgorithm.ES256K))
  }

  // TODO REMOVE
  // test("sign and verify plaintextMessage using JWKCruve") {
  //   val jwk: JWKCruve = JWKExamples.senderSecp256k1.fromJson[JWKCruve].toOption.get
  //   val jwsObject = jwk.sign(DIDCommExamples.plaintextMessageObj)
  //   assert(jwk.verify(jwsObject))
  //   assert(jwk.verify(JWMExample.example))
  // }

}
