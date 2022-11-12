package fmgp.multibase

object Base16Impl {

  def encode(data: Array[Byte], base16: Base16RFC4648): String =
    data.flatMap(b => byteToChars(b, base16.alphabet.toCharArray)).mkString

  private def byteToChars(byte: Byte, alphabet: Array[Char]): Array[Char] = Array(
    alphabet(MASK_4BITS(byte >>> 4)),
    alphabet(MASK_4BITS(byte))
  )

  def decode(data: String, base16: Base16RFC4648): Array[Byte] =
    data.toCharArray
      .grouped(2)
      .map(g => twoCharsToByte(g, base16.alphabetPos))
      .toArray

  private def twoCharsToByte(tuple: Array[Char], pos: Map[Char, Int]): Byte =
    (pos(tuple(0)) << 4 | pos(tuple(1))).toByte

}
