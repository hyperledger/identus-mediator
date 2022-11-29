package fmgp.crypto //FIXME

// sealed abstract class
// object CryptoFail {
package error {

  sealed trait DidFail extends Exception with Product with Serializable

  case class FailToParse(error: String) extends DidFail

  case class DidMethodNotSupported(method: String) extends DidFail

  // Error Crypto
  sealed trait CryptoFailed extends DidFail

  case object CryptoNotImplementedError extends CryptoFailed
  case object UnknownError extends CryptoFailed

  case object WrongKeysTypeCombination extends CryptoFailed

  case object EncryptionFailed extends CryptoFailed
  case object DecryptionFailed extends CryptoFailed

  case object NoKeys extends CryptoFailed
  case object IncompatibleKeys extends CryptoFailed
  case object MissingDecryptionKey extends CryptoFailed
  case object SignatureVerificationFailed extends CryptoFailed

  // Warn Crypto
  sealed trait CryptoWarn extends Product with Serializable // Exception with
  case class MissDecryptionKey(kid: String) extends CryptoWarn
  case class UncatchWarning[E <: CryptoWarn](warn: E) extends CryptoFailed

}
