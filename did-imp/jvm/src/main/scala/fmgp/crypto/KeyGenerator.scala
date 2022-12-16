package fmgp.crypto

import zio.json._

import zio._
import scala.util.Try

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator

import fmgp.crypto.error._
import fmgp.crypto.OKPPrivateKey

object KeyGenerator {
  def newX25519: Either[FailToGenerateKey, OKPPrivateKey] = Try {
    new OctetKeyPairGenerator(Curve.X25519).generate.toJSONString()
  }.toEither.left
    .map { case e => FailToGenerateKey(SomeThrowable(e)) }
    .flatMap {
      _.fromJson[OKPPrivateKey].left
        .map(strError => FailToGenerateKey(FailToParse(strError)))
    }

  def newEd25519: Either[FailToGenerateKey, OKPPrivateKey] = Try {
    new OctetKeyPairGenerator(Curve.Ed25519).generate.toJSONString()
  }.toEither.left
    .map { case e => FailToGenerateKey(SomeThrowable(e)) }
    .flatMap {
      _.fromJson[OKPPrivateKey].left
        .map(strError => FailToGenerateKey(FailToParse(strError)))
    }

  def makeX25519: IO[FailToGenerateKey, OKPPrivateKey] = ZIO.fromEither(newX25519)
  def makeEd25519: IO[FailToGenerateKey, OKPPrivateKey] = ZIO.fromEither(newEd25519)
}
