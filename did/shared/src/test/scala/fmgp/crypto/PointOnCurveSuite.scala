package fmgp.crypto

import munit._
import zio._
import zio.json._
import fmgp.util.Base64

/** didJVM/testOnly fmgp.crypto.PointOnCurveSuite
  *
  * @see
  *   https://8gwifi.org/jwkfunctions.jsp to generate keys for testing
  */
class PointOnCurveSuite extends ZSuite {

  val ex_P_256 = Seq(
    """{"kty":"EC","d":"MGcRNu2WJbyfHaYAIsWTknKlh_HQeAj1H6YaUxundx4","crv":"P-256","x":"csHqVpY_qD7rkQ2bNE0O3D8d94SqoMpixMKm29JCrdk","y":"AgTLnE5G83QSdzEB3xFxcjOCtqDvEAsaw_aEgwGNq9M"}""",
    """{"kty":"EC","d":"XvAgb_5RMgb7DQdNCojxsJPH6eW_IrnuWPQYdfZ7aw4","crv":"P-256","x":"1hTqxntzuO74KTqKGB3fWRENlyZdy7c3Xfxw8Y1gKfk","y":"rayv0ou4Txx5l8Hg2gG0_SaJOTs08BrWATzTRm7vo0w"}""",
    """{"kty":"EC","d":"DqTJSUHiZijdHrncqx103oxKm3e7-S4sGYx--wBLK1s","crv":"P-256","x":"O5gSx5fsELH07GBO6bow15obu16Emt_GJvpIJs_ouWY","y":"OYmbEOLy8DH7ColYN_Mjer0BDo2YbDoHTPl_GyvXde8"}""",
  )

  val ex_P_384 = Seq(
    """{"kty":"EC","d":"elphNnyeN6o0ta35EJvuwiI2X1KxMzSTfBl1g94c3zvOC_IpnhxYC7od6Z0isET9","crv":"P-384","x":"l1MjAJ1g5-PBAeQodS8hScqEz_W2gVGR5TjYpSxH4KG5NS8olhxcSQbg2IHxV2Ek","y":"sk4gU7qmLiy81Gr2CrRdxnvLy4oHlHCb01ALUxnYoTWn2144EsyBGYPeev5rpduO"}""",
    """{"kty":"EC","d":"wiJEhj6NScu1Ea06INb449rOBVY4vNC0f_ztGbShFFcQHEZayY3SAGtGerPPKYXm","crv":"P-384","x":"nYfMMyQy7PCisTX96AY1ZYBrRsKLfu0E2bELiY4TMdPziWBD0UfuJ2YPzZhyQmdJ","y":"iRtdQBQOPKFwyCdrX14GmzG5Zt9PugWiH3-kAwoDEaBsegBIWBnOo4HwpKBbyfYR"}""",
    """{"kty":"EC","d":"KOZjS-lFbZ8ys6Iy6XJZtGGar8MDSjv4necJNkwahfvQE4NnTHR099rn2MRQSPZT","crv":"P-384","x":"PHq7yYtq0snjIXKzmASBVPH28Ee_mY6zZkZxzQUc4JnTtTYdX9wkpKb8PZs-zC9i","y":"RbSi4ChOt77zwX3FN0Ud39C5iNnQ15cq0bl8a7Iwh6m_MEs0ANYxlpB6L7Zmpu__"}""",
  )

  val ex_P_521 = Seq(
    """{"kty":"EC","d":"Adbq7xMdozrUl9x-iTW9pfICjpY6wJwtJZUPPNBBtDiWmeHVTUO6PmtdHGkm5UOUlrjzVvTn-490JALt1MPYDUaJ","crv":"P-521","x":"AWIWpDVjcxvJMLxExkv7v33J5Omo8UkuDVXNDGHIt2uuTGZa8we5_nak2aoXxNNv0asscum5SIf11ncJ4QPyQbG8","y":"AR_QusMzBYzc0fYULxJ3pljCyLY29sFGwIWcH5ir4IfZ_5W-IWZvdIsHCJyrrOYQli2noXrhqwQkrJ5AckseYSQn"}""",
    """{"kty":"EC","d":"AZXxWxM2EsINrHSM4asb06TAt7KMaYhuBi7Nw64MqWY58Lr4DiG1ynNTl4ffhCkNbnANnZ6mZcQwDMcIHK2oDBAT","crv":"P-521","x":"AcBTk8Yq3OHJYrZiShcJLzsJcYEUTbO5qkBbaid-oeJiG5vX-IUXh-TiM9Wxzk-Xp6NN5RjInnijiQv0kC3QFOrc","y":"Af1kfbfrgjm93fWuWBn9jKoR3Z4Lgsp8sQ7kxUNWK6Jc7SZOD1nYW9uYz9etEyuLSrIC_2YpNQXNHEMNO_OLVoda"}""",
    """{"kty":"EC","d":"AZ1gn23K5m4Q-V9XzkMG6rFfX4x-2Q6ReFCgpkf1v5i9NYrhiMndgt2O8VphbgZ8wNeqgmVgzn69RoaGR0uTzYjQ","crv":"P-521","x":"AGngjs3yClqYWs5rLFnO3P1ZFqH6jSQA-ux5N6LU2yR0FJvRJdcZYjOeUU-p3iTBm9oxbPYAwY6B04N4QL1G1tox","y":"ATb86JB-Pc0DrlVwKhm61hcn76g7vIFCnE3lT_AjMrE4TTjsJdLUzC73WhaK-12StOF4Hy2Nzor3jRezM-cGSrCN"}""",
  )

  test("is point on curve Secp256k1") {
    val key = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    assert(key.isPointOnCurve)
    assert(PointOnCurve.isPointOnCurveSecp256k1(key.xNumbre, key.yNumbre))
    assert(!PointOnCurve.isPointOnCurveP_256(key.xNumbre, key.yNumbre))
    assert(!PointOnCurve.isPointOnCurveP_384(key.xNumbre, key.yNumbre))
    assert(!PointOnCurve.isPointOnCurveP_521(key.xNumbre, key.yNumbre))
  }

  test("is point on curve P_256") {
    ex_P_256
      .map(_.fromJson[ECPrivateKey].toOption.get)
      .foreach { key =>
        assert(key.isPointOnCurve)
        assert(!PointOnCurve.isPointOnCurveSecp256k1(key.xNumbre, key.yNumbre))
        assert(PointOnCurve.isPointOnCurveP_256(key.xNumbre, key.yNumbre))
        assert(!PointOnCurve.isPointOnCurveP_384(key.xNumbre, key.yNumbre))
        assert(!PointOnCurve.isPointOnCurveP_521(key.xNumbre, key.yNumbre))
      }
  }

  test("is point on curve P_384") {
    ex_P_384
      .map(_.fromJson[ECPrivateKey].toOption.get)
      .foreach { key =>
        assert(key.isPointOnCurve)
        assert(!PointOnCurve.isPointOnCurveSecp256k1(key.xNumbre, key.yNumbre))
        assert(!PointOnCurve.isPointOnCurveP_256(key.xNumbre, key.yNumbre))
        assert(PointOnCurve.isPointOnCurveP_384(key.xNumbre, key.yNumbre))
        assert(!PointOnCurve.isPointOnCurveP_521(key.xNumbre, key.yNumbre))
      }
  }

  test("is point on curve P_521") {
    ex_P_521
      .map(_.fromJson[ECPrivateKey].toOption.get)
      .foreach { key =>
        assert(key.isPointOnCurve)
        assert(!PointOnCurve.isPointOnCurveSecp256k1(key.xNumbre, key.yNumbre))
        assert(!PointOnCurve.isPointOnCurveP_256(key.xNumbre, key.yNumbre))
        assert(!PointOnCurve.isPointOnCurveP_384(key.xNumbre, key.yNumbre))
        assert(PointOnCurve.isPointOnCurveP_521(key.xNumbre, key.yNumbre))
      }
  }
}
