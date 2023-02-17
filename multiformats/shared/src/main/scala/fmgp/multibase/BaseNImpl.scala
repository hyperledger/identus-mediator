package fmgp.multibase

import scala.annotation.tailrec

object BaseNImpl {

  def encode(base: Base, data: Array[Byte]): String =
    if (data.isEmpty) ""
    else {
      val alphabet: Array[Char] = base.alphabet.toCharArray
      val baseSize = base.alphabet.length
      val ZERO = alphabet(0)

      @tailrec
      def buildBase(res: String, bi: BigInt): String =
        if (bi <= 0) res
        else buildBase(alphabet((bi % baseSize).toInt).toString + res, bi / baseSize)

      @tailrec
      def confirmZeroByte(res: String, bytes: Array[Byte], idx: Int): String =
        if (bytes(idx) != 0 || idx >= bytes.length) res
        else confirmZeroByte(ZERO.toString + res, bytes, idx + 1)

      confirmZeroByte(buildBase("", BigInt(1, data)), data, 0)
    }

  def decode(base: Base, data: String): Array[Byte] = {
    val alphabet: Array[Char] = base.alphabet.toCharArray
    val baseSize = base.alphabet.length
    val ZERO = alphabet(0)

    @tailrec
    def restoreBigInt(chars: Array[Char], bi: BigInt, idx: Int): BigInt =
      if (idx >= chars.length) bi
      else {
        val i: Int = alphabet.zipWithIndex.find(t => t._1 == chars(idx)).map(_._2).get
        restoreBigInt(chars, bi * baseSize + i, idx + 1)
      }

    val zeroes = data.takeWhile(_ == ZERO).map(_ => 0: Byte).toArray
    val trim = data.dropWhile(_ == ZERO).toCharArray

    zeroes ++ restoreBigInt(trim, 0, 0).toByteArray
  }

}
