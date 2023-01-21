package fmgp.did.comm

import fmgp.did.comm._
import zio._

/*
sealed trait ZMsg[+F, +T] { self =>
  def sign[F1 >: F](from: F1): ZMsg[F1, T] = ZMsg.Sign(self, from)
  def anon[T1 >: T](to: T1): ZMsg[F, T1] = ZMsg.Anon(self, to)
  def auth[F1 >: F, T1 >: T](from: F1, to: T1): ZMsg[F1, T1] = ZMsg.Auth(self, from, to)
}
object ZMsg {

  def mT[T](to: T): ZMsg[Nothing, T] = PlainTo(to)
  def mF[F](from: F): ZMsg[F, Nothing] = PlainFrom(from)
  def mFT[F, T](from: F, to: T): ZMsg[F, T] = PlainFromTo(from, to)

  private final case class PlainTo[T](to: T) extends ZMsg[Nothing, T]
  private final case class PlainFrom[F](from: F) extends ZMsg[F, Nothing]
  private final case class PlainFromTo[F, T](from: F, to: T) extends ZMsg[F, T]

  private final case class Sign[F, T, F1 <: F](self: ZMsg[F, T], from: F1) extends ZMsg[F1, T]
  private final case class Anon[F, T, T1 <: T](self: ZMsg[F, T], to: T1) extends ZMsg[F, T1]
  private final case class Auth[F, T, F1 <: F, T1 <: T](self: ZMsg[F, T], from: F1, to: T1) extends ZMsg[F1, T1]

  private final case class Sign2[F, T, F1 <: F](self: ZMsg[F1, T], from: F) extends ZMsg[F, T]
  private final case class Anon2[F, T, T1 <: T](self: ZMsg[F, T1], to: T) extends ZMsg[F, T]
  private final case class Auth2[F, T, F1 <: F, T1 <: T](self: ZMsg[F, T], from: F1, to: T1) extends ZMsg[F1, T1]

  val from = FROM("did:ex:1")
  val to = TO("did:ex:2") // Seq(TO("did:ex:2"))

  val aa = mFT(new A, new B).anon(new B).anon(new C) // .anon(new A)
  val bb = mFT(Checked(from), Uncheck(to)).anon(Checked(to))
  val bbb = mFT(CheckedFROM(from), UncheckTO(to)).anon(CheckedTO(to))
  // val aa: ZMsg[Checked[FROM], Checked[TO]] = mFT(Checked(from), Uncheck(to)) // .anon(Checked(to))
  // val cc: ZMsg[Uncheck[FROM], Checked[TO]] = mFT(Checked(from), Uncheck(to)) // .anon(Checked(to))

}*/

/** Try to make some Business Logic to the type system
  *
  * TODO The goal is make sure the
  *
  * WIP & EXPERIMENTAL
  */
sealed trait OpsMeta[-F, -T, +HF, +HT] { self =>
  // def sign[F1 <: F](from: F1): OpsMeta[F1, T] = ??? // OpsMeta.Sign(self, from)
  // def anon[T1 <: T](to: T1): OpsMeta[F, T1] = ??? // OpsMeta.Anon(self, to)
  // def auth[F1 <: F, T1 <: T](from: F1, to: T1): OpsMeta[F1, T1] = ??? // OpsMeta.Auth(self, from, to)
  // def verify[FHout >: HF <: F](from: FHout): OpsMeta[Any, T, FHout, HT] = OpsMeta.Verified(self, from)
  def verify[FHout >: HF <: F](from: FHout): OpsMeta[Any, T, FHout, HT] = OpsMeta.Verified(self, from)
  def anonDecrypt[Tout >: HT <: T](to: Tout): OpsMeta[F, Any, HF, Tout] = OpsMeta.AnonDecrypt(self, to)
  def authDecrypt[Fout >: HF <: F, Tout >: HT <: T](from: Fout, to: Tout): OpsMeta[Any, Any, Fout, Tout] =
    OpsMeta.AuthDecrypt(self, from, to)
  // def next1[Fin >: Fout, Fout <: F, Tin >: Tout, Tout <: T, HF, HT](
  //     that: => OpsMeta[Fin, Tin, HF, HT]
  // ): OpsMeta[Fout, Tout, HF, HT] = OpsMeta.Next(self, that)

  def next[
      F1 <: F,
      T1 <: T,
      HF1 >: HF,
      HT1 >: HT
  ](that: => OpsMeta[F1, T1, HF1, HT1]): OpsMeta[F1, T1, HF1, HT1] = OpsMeta.Fold(self, that)

}

object OpsMeta {
  def singed(from: FROM): OpsMeta[FROM, Any, Nothing, Nothing] = Sing(from)
  def anoned(to: Seq[TO]): OpsMeta[Any, Seq[TO], Nothing, Nothing] = Anon(to)
  def authenticated(from: FROM, to: Seq[TO]): OpsMeta[FROM, Seq[TO], Nothing, Nothing] = Auth(from, to)

  private final case class Sing[F](from: F) extends OpsMeta[F, Any, Nothing, Nothing]
  private final case class Anon[T](to: T) extends OpsMeta[Any, T, Nothing, Nothing]
  private final case class Auth[F, T](from: F, to: T) extends OpsMeta[F, T, Nothing, Nothing]

  private final case class Verified[
      F,
      F1 >: F,
      HF,
      HFout <: F1,
      T,
      HT
  ](self: OpsMeta[F, T, HF, HT], from: F1)
      extends OpsMeta[Any, T, HFout, HT]
  private final case class AnonDecrypt[
      F,
      HF,
      T,
      T1 >: T,
      HT,
      HTout <: T1,
  ](self: OpsMeta[F, T, HF, HT], to: T1)
      extends OpsMeta[F, Any, HF, HTout]
  private final case class AuthDecrypt[
      F,
      F1 >: F,
      HF,
      HFout <: F1,
      T,
      T1 >: T,
      HT,
      HTout <: T1,
  ](self: OpsMeta[F, T, HF, HT], from: F1, to: T1)
      extends OpsMeta[Any, Any, HFout, HTout]

  // private final case class Fold[F, T, HF, HT](
  //     self: OpsMeta[F, T, HF, HT],
  //     that: OpsMeta[F, T, HF, HT]
  // ) extends OpsMeta[F, T, HF, HT]
  final case class Fold[
      F,
      F1 >: F,
      F2 >: F,
      T,
      T1 >: T,
      T2 >: T,
      HF,
      HT
  ](self: OpsMeta[F1, T1, HF, HT], that: OpsMeta[F2, T2, HF, HT])
      extends OpsMeta[F, T, HF, HT]
  // final case class Next[-F, -T, Fin >: Fout, Fout <: F, Tin >: Tout, Tout <: T, HF, HT](
  //     self: OpsMeta[F, T, HF, HT],
  //     that: OpsMeta[Fin, Tin, HF, HT]
  // ) extends OpsMeta[Fout, Tout, HF, HT]

  val from = FROM("did:ex:1")
  val to = TO("did:ex:2") // Seq(TO("did:ex:2"))

  val xx = anoned(Seq(to))

}

object OpsMetaSuite {

  class A
  class B extends A
  class C extends B

  class A1
  class B1 extends A1
  class C1 extends B1

  val self1: OpsMeta[A, Any, Nothing, Nothing] = ???
  val that1: OpsMeta[B, Any, Nothing, Nothing] = ???
  val other1: OpsMeta[C, Any, Nothing, Nothing] = ???
  val a = that1.next(self1)
  val aa = that1.next(that1)
  val aaa = that1.next(other1)
  // val t = Fold(that1, self1): OpsMeta[B, Any, Nothing, Nothing]
  // val tt = Fold(that1, that1): OpsMeta[B, Any, Nothing, Nothing]
  // val ttt = Fold(that1, other1): OpsMeta[C, Any, Nothing, Nothing]

  val self2: OpsMeta[Any, Any, A, Nothing] = ???
  val that2: OpsMeta[Any, Any, B, Nothing] = ???
  val other2: OpsMeta[Any, Any, C, Nothing] = ???
  val b = that2.next(self2)
  val bb = that2.next(that2)
  val bbb = that2.next(other2)

  val self3: OpsMeta[A, Any, A, Nothing] = ???
  val that3: OpsMeta[A, Any, B, Nothing] = ???
  val other3: OpsMeta[A, Any, C, Nothing] = ???
  val c = that3.next(self3)
  val cc = that3.next(that3)
  val ccc = that3.next(other3)

  val x11 = self1.verify(B())
  val x12 = that1.verify(B())
  // val x13 = other1.verify(B()) // Must not compile
  val x21 = self1.anonDecrypt(B())
  val x22 = that1.anonDecrypt(B())
  val x23 = other1.anonDecrypt(B())
  val x31 = self1.authDecrypt(B(), B())
  val x32 = that1.authDecrypt(B(), B())
  // val x33 = other1.authDecrypt(B(), B()) // Must not compile

  val y11 = self2.verify(B())
  val y12 = that2.verify(B())
  val y13 = other2.verify(B())
  val y21 = self2.anonDecrypt(B())
  val y22 = that2.anonDecrypt(B())
  val y23 = other2.anonDecrypt(B())
  val y31 = self2.authDecrypt(B(), B())
  val y32 = that2.authDecrypt(B(), B())
  val y33 = other2.authDecrypt(B(), B())

  val z11 = self3.verify(B())
  val z12 = that3.verify(B())
  val z13 = other3.verify(B())
  val z21 = self3.anonDecrypt(B())
  val z22 = that3.anonDecrypt(B())
  val z23 = other3.anonDecrypt(B())
  val z31 = self3.authDecrypt(B(), B())
  val z32 = that3.authDecrypt(B(), B())
  val z33 = other3.authDecrypt(B(), B())

  // val from = FROM("did:ex:1")
  // val to = Seq(TO("did:ex:2"))
  // // https://identity.foundation/didcomm-messaging/spec/#iana-media-types

  // val values = Seq(
  //   // plaintext
  //   SecureTo(to),
  //   SecureFrom(from),
  //   SecureFromTo(from, to),

  //   // // signed(plaintext)
  //   // SignT(SecureFrom(from), from),
  //   // SignT(SecureFromTo(from, to), from),
  //   // // Does not compile SignT(SecureTo(to), from),
  //   // // Does not compile Sign(Unknown, from),

  //   // anoncrypt(plaintext)
  //   AnonT(SecureTo(to), to),
  //   AnonT(SecureFrom(from), to), // DO NOT COMPILE PLZ
  //   AnonT(SecureFromTo(from, to), to),

  //   // authcrypt(plaintext)
  //   // AuthT(Unsecure, from, Seq(to)),
  //   // AnonT(SignT(Unsecure, from), Seq(to)), // anoncrypt(sign(plaintext))
  //   // AuthT(SignT(Unsecure, from), from, Seq(to)), // authcrypt(sign(plaintext))  //SHOULD NOT be emitted
  //   // AnonT(AuthT(Unsecure, from, Seq(to)), Seq(to)), // anoncrypt(authcrypt(plaintext))
  // )

}
// sealed trait Maybe[-A]
// object Absent extends Maybe[Nothing]
// sealed class Uncheck[A](v: A) extends Maybe[A]
// sealed class Checked[A](v: A) extends Uncheck[A](v)

// sealed trait MaybeFROM
// object AbsentFROM extends MaybeFROM
// sealed class UncheckFROM(v: FROM) extends MaybeFROM
// sealed class CheckedFROM(v: FROM) extends UncheckFROM(v)

// sealed trait MaybeTO
// object AbsentTO extends MaybeTO
// sealed class UncheckTO(v: TO) extends MaybeTO
// sealed class CheckedTO(v: TO) extends UncheckTO(v)
