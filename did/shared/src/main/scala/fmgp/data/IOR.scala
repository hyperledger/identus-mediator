package fmgp.data

sealed abstract class IOR[+L, +R] extends Product with Serializable {
  // def leftMap[X](f: L => X): IOR[X, R]
  def rightMap[X](f: R => X): IOR[L, X]
}

object IOR {
  final case class Left[+L](l: L) extends IOR[L, Nothing] {
    def rightMap[X](f: _ => X): IOR[L, X] = this
  }
  final case class Right[+R](r: R) extends IOR[Nothing, R] {
    def rightMap[X](f: R => X): IOR[Nothing, X] = Right(f(r))
  }
  final case class Both[+L, +R](l: L, r: R) extends IOR[L, R] {
    def rightMap[X](f: R => X): IOR[L, X] = Both(l, f(r))
  }

  def left[L, R](l: L): IOR[L, R] = IOR.Left(l)
  def right[L, R](r: R): IOR[L, R] = IOR.Right(r)
  def both[L, R](l: L, r: R): IOR[L, R] = IOR.Both(l, r)
}
