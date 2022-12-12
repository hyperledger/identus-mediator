package fmgp.crypto

import munit._

import zio._
import zio.json._

/**   - didImpJVM/testOnly fmgp.crypto.KeyGeneratorSuite
  *   - didImpJS/testOnly fmgp.crypto.KeyGeneratorSuite
  */
class KeyGeneratorSuite extends ZSuite {

  testZ("makeX25519".tag(fmgp.JsUnsupported)) {
    for {
      _ <- ZIO.unit
      key <- KeyGenerator.makeX25519
      _ = assertEquals(key.kty, KTY.OKP)
      _ = assertEquals(key.crv, Curve.X25519)
    } yield ()
  }

  testZ("makeEd25519".tag(fmgp.JsUnsupported)) {
    for {
      _ <- ZIO.unit
      key <- KeyGenerator.makeEd25519
      _ = assertEquals(key.kty, KTY.OKP)
      _ = assertEquals(key.crv, Curve.Ed25519)
    } yield ()
  }
}
