package fmgp.multibase

import scala.annotation.tailrec

object Base32Impl {
  // operator precedence in descending order: >>> or <<, &, |
  private val NumGroupsBeforeEncodeInLeastCommonLength: Int = 5
  private val NumGroupsAfterEncodeInLeastCommonLength: Int = 8

  /** Encode the given data as an Array of Bytes into a String, based on specified Base32 version, which has to be a
    * Base32RFC4648.
    */
  def encode(data: Array[Byte], base32: Base32RFC4648): String =
    if (data.isEmpty) ""
    else
      data
        .grouped(NumGroupsBeforeEncodeInLeastCommonLength)
        .map(g => encodeBytes(g, base32.alphabet.toCharArray, base32.pad))
        .mkString

  private def encodeBytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    group.length match {
      case 1 => encode1Bytes(group, alphabet, pad)
      case 2 => encode2Bytes(group, alphabet, pad)
      case 3 => encode3Bytes(group, alphabet, pad)
      case 4 => encode4Bytes(group, alphabet, pad)
      case 5 => encode5Bytes(group, alphabet)
    }

  private def encode1Bytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    Seq(
      alphabet(MASK_5BITS(group(0) >>> 3)), // 5
      alphabet(MASK_3BITS(group(0)) << 2) // 3
    ).mkString + pad.map(_.toString).getOrElse("") * 6

  private def encode2Bytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    Seq(
      alphabet(MASK_5BITS(group(0) >>> 3)), // 5
      alphabet(MASK_3BITS(group(0)) << 2 | MASK_2BITS(group(1) >>> 6)), // 3 2
      alphabet(MASK_5BITS(group(1) >>> 1)), //   5
      alphabet(MASK_1BITS(group(1)) << 4), //   1
    ).mkString + pad.map(_.toString).getOrElse("") * 4

  private def encode3Bytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    Seq(
      alphabet(MASK_5BITS(group(0) >>> 3)), // 5
      alphabet(MASK_3BITS(group(0)) << 2 | MASK_2BITS(group(1) >>> 6)), // 3 2
      alphabet(MASK_5BITS(group(1) >>> 1)), //   5
      alphabet(MASK_1BITS(group(1)) << 4 | MASK_4BITS(group(2) >>> 4)), //   1 4
      alphabet(MASK_4BITS(group(2)) << 1) //     4
    ).mkString + pad.map(_.toString).getOrElse("") * 3

  private def encode4Bytes(group: Array[Byte], alphabet: Array[Char], pad: Option[Char]): String =
    Seq(
      alphabet(MASK_5BITS(group(0) >>> 3)), // 5
      alphabet(MASK_3BITS(group(0)) << 2 | MASK_2BITS(group(1) >>> 6)), // 3 2
      alphabet(MASK_5BITS(group(1) >>> 1)), //   5
      alphabet(MASK_1BITS(group(1)) << 4 | MASK_4BITS(group(2) >>> 4)), //   1 4
      alphabet(MASK_4BITS(group(2)) << 1 | MASK_1BITS(group(3) >>> 7)), //     4 1
      alphabet(MASK_5BITS(group(3) >>> 2)), //       5
      alphabet(MASK_2BITS(group(3)) << 3) //       2
    ).mkString + pad.map(_.toString).getOrElse("")

  private def encode5Bytes(group: Array[Byte], alphabet: Array[Char]): String =
    Seq(
      alphabet(MASK_5BITS(group(0) >>> 3)), // 5
      alphabet(MASK_3BITS(group(0)) << 2 | MASK_2BITS(group(1) >>> 6)), // 3 2
      alphabet(MASK_5BITS(group(1) >>> 1)), //   5
      alphabet(MASK_1BITS(group(1)) << 4 | MASK_4BITS(group(2) >>> 4)), //   1 4
      alphabet(MASK_4BITS(group(2)) << 1 | MASK_1BITS(group(3) >>> 7)), //     4 1
      alphabet(MASK_5BITS(group(3) >>> 2)), //       5
      alphabet(MASK_2BITS(group(3)) << 3 | MASK_3BITS(group(4) >>> 5)), //       2 3
      alphabet(MASK_5BITS(group(4))) //         5
    ).mkString

  /** Decode the given data as a String into a Byte Array, based on specified Base32 version, which has to be a
    * Base32RFC4648.
    */
  def decode(data: String, base32: Base32RFC4648): Array[Byte] = {
    val chars = data.toCharArray
    val length = chars.length
    val pads = if (base32.pad.isEmpty) 0 else numPads(chars, 0, length - 1, base32.pad.get)
    val pos = base32.alphabetPos
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
      case 5 => decode5Bytes(group, pos)
      case 6 => decode6Bytes(group, pos)
      case 7 => decode7Bytes(group, pos)
      case 8 => decode8Bytes(group, pos)
    }

  private def decode8Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 3 | MASK_3BITS(pos(group(1)) >>> 2)).toByte, // 5 3
    (MASK_2BITS(pos(group(1))) << 6 | pos(group(2)) << 1 | MASK_1BITS(pos(group(3)) >>> 4)).toByte, //   2 5 1
    (MASK_4BITS(pos(group(3))) << 4 | MASK_4BITS(pos(group(4)) >>> 1)).toByte, //       4 4
    (MASK_1BITS(pos(group(4))) << 7 | pos(group(5)) << 2 | MASK_2BITS(pos(group(6)) >>> 3)).toByte, //         1 5 2
    (MASK_3BITS(pos(group(6))) << 5 | pos(group(7))).toByte //             3 5
  )

  private def decode7Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 3 | MASK_3BITS(pos(group(1)) >>> 2)).toByte, // 5 3
    (MASK_2BITS(pos(group(1))) << 6 | pos(group(2)) << 1 | MASK_1BITS(pos(group(3)) >>> 4)).toByte, //   2 5 1
    (MASK_4BITS(pos(group(3))) << 4 | MASK_4BITS(pos(group(4)) >>> 1)).toByte, //       4 4
    (MASK_1BITS(pos(group(4))) << 7 | pos(group(5)) << 2 | MASK_2BITS(pos(group(6)) >>> 3)).toByte //         1 5 2
  )

  private def decode6Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 3 | MASK_3BITS(pos(group(1)) >>> 2)).toByte, // 5 3
    (MASK_2BITS(pos(group(1))) << 6 | pos(group(2)) << 1 | MASK_1BITS(pos(group(3)) >>> 4)).toByte, //   2 5 1
    (MASK_4BITS(pos(group(3))) << 4 | MASK_4BITS(pos(group(4)) >>> 1)).toByte //       4 4
  )

  private def decode5Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 3 | MASK_3BITS(pos(group(1)) >>> 2)).toByte, // 5 3
    (MASK_2BITS(pos(group(1))) << 6 | pos(group(2)) << 1 | MASK_1BITS(pos(group(3)) >>> 4)).toByte, //   2 5 1
    (MASK_4BITS(pos(group(3))) << 4 | MASK_4BITS(pos(group(4)) >>> 1)).toByte //       4 4
  )

  private def decode4Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 3 | MASK_3BITS(pos(group(1)) >>> 2)).toByte, // 5 3
    (MASK_2BITS(pos(group(1))) << 6 | pos(group(2)) << 1 | MASK_1BITS(pos(group(3)) >>> 4)).toByte //   2 5 1
  )

  private def decode3Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 3 | MASK_3BITS(pos(group(1)) >>> 2)).toByte // 5 3
  )

  private def decode2Bytes(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array(
    (pos(group(0)) << 3 | MASK_3BITS(pos(group(1)) >>> 2)).toByte // 5 3
  )

  private def decode1Byte(group: Array[Char], pos: Map[Char, Int]): Array[Byte] = Array()

}
