package fmgp.crypto //FIXME

// sealed abstract class
// object CryptoFail {
package error {

  sealed trait DidFail // extends Exception with Product with Serializable
  case class DidException(error: FailToParse) extends Exception(error.error) // with DidFail

  case class FailToParse(error: String) extends DidFail

  case class DidMethodNotSupported(method: String) extends DidFail

  // ####################
  // ### Error Crypto ###
  // ####################
  sealed trait CryptoFailed extends DidFail
  case class FailToGenerateKey(origin: DidFail) extends CryptoFailed

  case object CryptoNotImplementedError extends CryptoFailed
  case object UnknownError extends CryptoFailed
  case class SomeThrowable(throwable: Throwable) extends CryptoFailed {
    override def toString(): String = throwable.getClass.getName() + ":" + throwable.getMessage
  }
  case class CryptoErrorCollection[E <: CryptoFailed](errors: Seq[E]) extends CryptoFailed
  object CryptoErrorCollection {
    def unfold[E <: CryptoFailed, A](x: Seq[Either[E, A]]): Either[CryptoFailed, Seq[A]] =
      x.partition(_.isLeft) match {
        case (l, r) if l.isEmpty => Right(r.map(_.right.get))
        case (l, _) =>
          val tmp = l.map(_.left.get)
          if (tmp.size == 1) Left(tmp.head)
          else Left(CryptoErrorCollection(tmp))
      }
  }
  case class CryptoFailToParse(error: String) extends CryptoFailed

  case object KeyMissingEpkJWEHeader extends CryptoFailed // TODO make it time safe

  /* EX: Curve of public key does not match curve of private key */
  sealed trait CurveError extends CryptoFailed
  case class WrongCurve(obtained: Curve, expected: Set[Curve]) extends CurveError
  case class MissingCurve(expected: Set[Curve]) extends CurveError
  case class MultiCurvesTypes(obtained: Set[Curve], expected: Set[Curve]) extends CurveError
  case object WrongKeysTypeCombination extends CurveError

  case object EncryptionFailed extends CryptoFailed
  case object DecryptionFailed extends CryptoFailed

  case object NoKeys extends CryptoFailed
  case class PointNotOnCurve(error: String) extends CryptoFailed
  case object IncompatibleKeys extends CryptoFailed
  case object MissingDecryptionKey extends CryptoFailed
  case object SignatureVerificationFailed extends CryptoFailed
  case object MACCheckFailed extends CryptoFailed
  case object MultiDifferentResults extends CryptoFailed
  case object ZeroResults extends CryptoFailed

  // Warn Crypto
  sealed trait CryptoWarn extends Product with Serializable // Exception with
  case class MissDecryptionKey(kid: String) extends CryptoWarn
  case class UncatchWarning[E <: CryptoWarn](warn: E) extends CryptoFailed

}
