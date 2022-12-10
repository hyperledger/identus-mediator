package fmgp.crypto

import java.security.spec.ECParameterSpec
import java.security.spec.ECFieldFp

/** Intended to prevent Invalid Curve Attack for:
  *   - P-256 - https://neuromancer.sk/std/nist/P-256
  *   - P-384 - https://neuromancer.sk/std/nist/P-384
  *   - P-521 - https://neuromancer.sk/std/nist/P-521 https://www.rfc-editor.org/rfc/rfc5480
  *   - secp256k1 - https://en.bitcoin.it/wiki/Secp256k1 // https://www.rfc-editor.org/rfc/rfc8812
  */
object PointOnCurve {
  // /** isPointOnCurve25519 Ensure the following condition is met:
  //   * {{{
  //   *   (y^2) mod p = (x^3 + ax + b) mod p
  //   *
  //   *   # For Curve25519 // https://neuromancer.sk/std/other/Curve25519
  //   *   a = 486662     # 0x76d06
  //   *   b = 1          # 0x01
  //   *   p = 2^255 - 19 # 57896044618658097711785492504343953926634992332820282019728792003956564819949 // 0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed
  //   * }}}
  //   *
  //   * X25519 - https://neuromancer.sk/std/other/Curve25519 and https://www.rfc-editor.org/rfc/rfc7748#page-4
  //   *
  //   * Ed25519 - https://www.rfc-editor.org/rfc/rfc8032#page-9
  //   *
  //   * @param x
  //   * @param y
  //   * @param ecParameterSpec
  //   */
  // def isPointOnCurve25519(x: BigInt, y: BigInt) = {
  //   val a: BigInt = 486662
  //   // val b: BigInt = 1
  //   val p: BigInt = BigInt(2).pow(255) - 19
  //   val leftSide: BigInt = y.pow(2)
  //   val rightSide: BigInt = x.pow(3) + (a * x.pow(2)) + x
  //   println(leftSide)
  //   println(rightSide)
  //   leftSide.mod(p) == rightSide.mod(p)
  // }

  inline def isPointOnCurveSecp256k1(x: BigInt, y: BigInt) = {
    // y**2 == x**3 + 7
    val p: BigInt = BigInt(2).pow(256) - BigInt(2).pow(32) - 977
    val leftSide: BigInt = y.pow(2)

    val rightSide: BigInt = x.pow(3) + 7
    leftSide.mod(p) == rightSide.mod(p)
  }

  inline def isPointOnCurveP_256(x: BigInt, y: BigInt) = {
    val a: BigInt = -3
    val b: BigInt = BigInt("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16)
    val p: BigInt = BigInt(2).pow(256) - BigInt(2).pow(224) + BigInt(2).pow(192) + BigInt(2).pow(96) - 1
    val leftSide: BigInt = y.pow(2)
    val rightSide: BigInt = x.pow(3) + (a * x) + b
    leftSide.mod(p) == rightSide.mod(p)
  }

  inline def isPointOnCurveP_384(x: BigInt, y: BigInt) = {
    val a: BigInt = -3
    val b: BigInt =
      BigInt("b3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef", 16)
    val p: BigInt = BigInt(2).pow(384) - BigInt(2).pow(128) - BigInt(2).pow(96) + BigInt(2).pow(32) - 1
    val leftSide: BigInt = y.pow(2)
    val rightSide: BigInt = x.pow(3) + (a * x) + b
    leftSide.mod(p) == rightSide.mod(p)
  }

  inline def isPointOnCurveP_521(x: BigInt, y: BigInt) = {
    val a: BigInt = -3
    val b: BigInt = BigInt(
      "051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00",
      16
    )
    val p: BigInt = BigInt(2).pow(521) - 1
    val leftSide: BigInt = y.pow(2)
    val rightSide: BigInt = x.pow(3) + (a * x) + b
    leftSide.mod(p) == rightSide.mod(p)
  }

}
