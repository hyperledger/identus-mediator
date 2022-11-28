package fmgp.crypto

import zio._

import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.OctetKeyPair

import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.SignedMessage
import fmgp.crypto.UtilsJVM._

object PlatformSpecificOperations {

  def sign(key: PrivateKey, plaintext: PlaintextMessage): UIO[SignedMessage] =
    ZIO.succeed(key.match {
      case okp: OKPPrivateKey => okp.toJWK.sign(plaintext, key.jwaAlgorithmtoSign)
      case ec: ECPrivateKey   => ec.toJWK.sign(plaintext, key.jwaAlgorithmtoSign)
    })

  def verify(key: PublicKey, jwm: SignedMessage): UIO[Boolean] =
    ZIO.succeed(key.match {
      case okp: OKPPublicKey => okp.toJWK.verify(jwm, key.jwaAlgorithmtoSign)
      case ec: ECPublicKey   => ec.toJWK.verify(jwm, key.jwaAlgorithmtoSign)
    })

}
