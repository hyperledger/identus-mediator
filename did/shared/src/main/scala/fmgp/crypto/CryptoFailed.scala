package fmgp.crypto //FIXME

// sealed abstract class
// object CryptoFail {
package error {

  sealed trait DidFail extends Exception with Product with Serializable

  case class FailToParse(error: String) extends DidFail

  case class DidMethodNotSupported(method: String) extends DidFail

  // ####################
  // ### Error Crypto ###
  // ####################
  sealed trait CryptoFailed extends DidFail
  case class FailToGenerateKey(throwable: Throwable) extends CryptoFailed {
    override def getMessage(): String = throwable.getClass.getName() + ":" + throwable.getMessage
  }

  case object CryptoNotImplementedError extends CryptoFailed
  case object UnknownError extends CryptoFailed
  case class SomeThrowable(throwable: Throwable) extends CryptoFailed {
    override def getMessage(): String = throwable.getClass.getName() + ":" + throwable.getMessage
  }
  case class CryptoErrorCollection[E <: DidFail](errors: Seq[E]) extends CryptoFailed {
    override def getMessage(): String =
      s"CryptoErrorCollection(${errors.size}): ${errors.map(_.getMessage).mkString("; ")}"
  }
  object CryptoErrorCollection {
    def unfold[E <: DidFail, A](x: Seq[Either[E, A]]): Either[CryptoErrorCollection[E], Seq[A]] =
      x.partition(_.isLeft) match {
        case (l, r) if l.isEmpty => Right(r.map(_.right.get))
        case (l, _)              => Left(CryptoErrorCollection(l.map(_.left.get)))
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

  // Warn Crypto
  sealed trait CryptoWarn extends Product with Serializable // Exception with
  case class MissDecryptionKey(kid: String) extends CryptoWarn
  case class UncatchWarning[E <: CryptoWarn](warn: E) extends CryptoFailed

}
