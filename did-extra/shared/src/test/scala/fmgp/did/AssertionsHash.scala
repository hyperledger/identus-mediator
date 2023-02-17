package fmgp.did

import munit._
import zio.prelude.{Hash, Equal}

trait AssertionsHash extends Assertions {
  import munit.internal.console.{Lines, Printers, StackTraces}

  /** Asserts that two elements are equal according to the `Compare[A, B]` type-class.
    *
    * By default, uses `==` to compare values.
    */
  def assertEqualHash[A, B](
      obtained: A,
      expected: B,
      clue: => Any = "values has not the hash"
  )(using loc: Location, hashA: Hash[A], hashB: Hash[B], compare: Compare[Int, Int]): Unit = {
    StackTraces.dropInside {
      if (!compare.isEqual(hashA.hash(obtained), hashB.hash(expected))) {
        (obtained, expected) match {
          case (a: Array[_], b: Array[_]) if a.sameElements(b) =>
            failComparison(
              "arrays have the same elements but different reference equality. " +
                "Convert the arrays to a non-Array collection if you intend to assert the two arrays have the same elements. " +
                "For example, `assertEquals(a.toSeq, b.toSeq)",
              obtained,
              expected
            )
          case _ =>
        }
        compare.failEqualsComparison(
          hashA.hash(obtained),
          hashB.hash(expected),
          clue,
          loc,
          this
        )
      }
    }
  }

}
