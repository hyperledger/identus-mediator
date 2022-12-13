package fmgp

package object util {

  /** Use call valueOf of a enum inside of safeValueOf (and ONLY valueOf!)
    *
    * Use like this!!!
    * {{{
    *   fmgp.util.safeValueOf(A.valueOf(str))
    * }}}
    *
    * TODO make this a inline macro of Enum[A]
    */
  inline def safeValueOf[A](block: => A): Either[String, A] =
    scala.util.Try(block).toEither match
      case Right(value)                                       => Right(value)
      case Left(ex /*: java.lang.IllegalArgumentException*/ ) => Left(ex.getMessage)
}
