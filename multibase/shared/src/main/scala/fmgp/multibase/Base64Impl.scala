package fmgp.multibase

import scala.annotation.tailrec

object Base64Impl {
  // operator precedence in descending order: >>> or <<, &, |
  private val NumGroupsBeforeEncodeInLeastCommonLength: Int = 3
  private val NumGroupsAfterEncodeInLeastCommonLength: Int = 4

  /** Encode the given data as an Array of Bytes into a String, based on specified Base64 version, which has to be a
    * Base64RFC4648.
    */
  def encode(data: Array[Byte], base64: Base64RFC4648): String =
    if (data.isEmpty) ""
    else
      data
        .grouped(NumGroupsBeforeEncodeInLeastCommonLength)
        .map(g => encodeBytes(g, base64.alphabet.toCharArray, base64.pad))
        .mkString

  private def encodeBytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    group.length match {
      case 1 => encode1Bytes(group, alphabet, pad)
      case 2 => encode2Bytes(group, alphabet, pad)
      case 3 => encode3Bytes(group, alphabet, pad)
    }

  private def encode1Bytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    Seq(
      alphabet(MASK_6BITS(group(0) >>> 2)), // 6
      alphabet(MASK_2BITS(group(0)) << 4) // 2
    ).mkString + pad.map(_.toString).getOrElse("") * 2

  private def encode2Bytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    Seq(
      alphabet(MASK_6BITS(group(0) >>> 2)), // 6
      alphabet(MASK_2BITS(group(0)) << 4 | MASK_4BITS(group(1) >>> 4)), // 2 4
      alphabet(MASK_4BITS(group(1)) << 2) //   4
    ).mkString + pad.map(_.toString).getOrElse("")

  private def encode3Bytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    Seq(
      alphabet(MASK_6BITS(group(0) >>> 2)), // 6
      alphabet(MASK_2BITS(group(0)) << 4 | MASK_4BITS(group(1) >>> 4)), // 2 4
      alphabet(MASK_4BITS(group(1)) << 2 | MASK_2BITS(group(2) >>> 6)), //   4 2
      alphabet(MASK_6BITS(group(2))) //     6
    ).mkString

  /** Decode the given data as a String into a Byte Array, based on specified Base64 version, which has to be a
    * Base64RFC4648.
    */
  def decode(data: String, base64: Base64RFC4648): Array[Byte] = {
    val chars = data.toCharArray
    val length = chars.length
    val pads = if (base64.pad.isEmpty) 0 else numPads(chars, 0, length - 1, base64.pad.get)
    val pos = base64.alphabetPos
    chars
      .slice(0, length - pads)
      .grouped(NumGroupsAfterEncodeInLeastCommonLength)
      .map(g => decodeBytes(g, pos))
      .foldLeft(Array[Byte]())(_ ++ _)
  }

  @tailrec
  private def numPads(chars: Array[Char], pads: Int, last: Int, pad: Char): Int =
    if (last < 0 || pads >= NumGroupsBeforeEncodeInLeastCommonLength || chars(last) != pad) pads
    else numPads(chars, pads + 1, last - 1, pad)

  private def decodeBytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] =
    group.length match {
      case 1 => decode1Byte(group, pos)
      case 2 => decode2Bytes(group, pos)
      case 3 => decode3Bytes(group, pos)
      case 4 => decode4Bytes(group, pos)
    }

  private def decode4Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 2 | MASK_2BITS(pos(group(1)) >>> 4)).toByte, // 6 2
    (MASK_4BITS(pos(group(1))) << 4 | MASK_4BITS(pos(group(2)) >>> 2)).toByte, //   4 4
    (MASK_2BITS(pos(group(2))) << 6 | pos(group(3))).toByte //     2 6
  )

  private def decode3Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 2 | MASK_2BITS(pos(group(1)) >>> 4)).toByte, // 6 2
    (MASK_4BITS(pos(group(1))) << 4 | MASK_4BITS(pos(group(2)) >>> 2)).toByte //   4 4
  )

  private def decode2Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 2 | MASK_2BITS(pos(group(1)) >>> 4)).toByte // 6 2
  )

  private def decode1Byte(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array()

}
