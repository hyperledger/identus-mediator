package fmgp.crypto

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.StandardCharset
import com.nimbusds.jwt.*
import fmgp.did.comm.DIDCommExamples
import fmgp.did.comm.PlaintextMessageClass
import zio.json._

import java.math.BigInteger
import java.nio.charset.StandardCharsets

/* didJVM/Test/runMain fmgp.crypto.MainTestJVM */
object MainTestJVM {

  def compressPubKey(pubKey: BigInteger): String = {
    val pubKeyYPrefix: String = if (pubKey.testBit(0)) "03" else "02"
    val pubKeyHex: String = pubKey.toString(16);
    val pubKeyX: String = pubKeyHex.substring(0, 64);
    pubKeyYPrefix + pubKeyX;
  }

  def main(args: Array[String]) = {

    // val privKey: BigInteger = // N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA
    //   BigInt(
    //     "25078453526708925569941471116689713909532199220169070612570950033488200344352"
    //   ).bigInteger

    // Private key: 25078453526708925569941471116689713909532199220169070612570950033488200344352
    // Public key: c390da3018ce25743d1a9207596d71da65b83dc228c65125057e03c2d2299c81d0699343470ebaab84ca33557b240fb810e2a75a52f5ae60068702088605fca7

    // val ecJWK: ECKey = ECKey
    //   .Builder(
    //     Curve.SECP256K1,
    //     Base64URL("aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk"),
    //     Base64URL("JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk")
    //   )
    //   .keyID("did:example:alice#key-3")
    //   .d(Base64URL("N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA"))
    //   .build()

    // val ecPublicJWK: ECKey = ecJWK.toPublicJWK()

    // val jwsObject = ecJWK.sign(DIDCommExamples.plaintextMessageObj)
    // println("serialize: " + jwsObject)

    // println(ecPublicJWK.verify(jwsObject))
    // println(ecPublicJWK.verify(JWMExample.example))

  }

}
