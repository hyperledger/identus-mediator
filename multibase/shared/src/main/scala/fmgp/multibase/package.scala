package fmgp

package object multibase {

  val MASK_NBITS: (Int, Int) => Int = (b: Int, n: Int) => b & ((1 << n) - 1)

  val MASK_1BITS: Int => Int = (b: Int) => b & 1
  val MASK_2BITS: Int => Int = (b: Int) => MASK_NBITS(b, 2)
  val MASK_3BITS: Int => Int = (b: Int) => MASK_NBITS(b, 3)
  val MASK_4BITS: Int => Int = (b: Int) => MASK_NBITS(b, 4)
  val MASK_5BITS: Int => Int = (b: Int) => MASK_NBITS(b, 5)
  val MASK_6BITS: Int => Int = (b: Int) => MASK_NBITS(b, 6)

}
