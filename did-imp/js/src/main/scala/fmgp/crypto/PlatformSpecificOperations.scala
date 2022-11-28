package fmgp.crypto

import zio._

import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.SignedMessage
import fmgp.crypto.UtilsJS._
import fmgp.crypto.error._
import typings.jose.joseStrings.sha256

object PlatformSpecificOperations {
  def sign(key: PrivateKey, plaintext: PlaintextMessage): IO[CryptoFailed, SignedMessage] =
    key.sign(plaintext)
  def verify(key: PublicKey, jwm: SignedMessage): IO[CryptoFailed, Boolean] =
    key.verify(jwm)
  sha256
}
