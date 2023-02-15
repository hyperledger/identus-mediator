package fmgp.multibase

sealed abstract class Base(val name: String, val code: Char, val alphabet: String) {

  lazy val alphabetPos: Map[Char, Int] = (for (i <- alphabet.indices) yield alphabet(i) -> i).toMap

  def encode(data: Array[Byte]): Multibase = Multibase.encode(this, data)

  def encodeString(data: String): Multibase = Multibase.encodeString(this, data)

}

sealed class Base16RFC4648(
    override val name: String,
    override val code: Char,
    override val alphabet: String,
    val pad: Option[Char]
) extends Base(name, code, alphabet)

sealed class Base32RFC4648(
    override val name: String,
    override val code: Char,
    override val alphabet: String,
    val pad: Option[Char]
) extends Base(name, code, alphabet)

sealed class Base64RFC4648(
    override val name: String,
    override val code: Char,
    override val alphabet: String,
    val pad: Option[Char]
) extends Base(name, code, alphabet)

object Base {

  /** Reference: https://github.com/multiformats/multibase/blob/master/multibase.csv encoding codes name
    *
    * identity 0x00 8-bit binary (encoder and decoder keeps data unmodified) base1 1 unary tends to be 11111 base2 0
    * binary has 1 and 0 base8 7 highest char in octal base10 9 highest char in decimal base16 F, f highest char in hex
    * base32 B, b rfc4648 - no padding - highest letter base32pad C, c rfc4648 - with padding base32hex V, v rfc4648 -
    * no padding - highest char base32hexpad T, t rfc4648 - with padding base32z h z-base-32 - used by Tahoe-LAFS -
    * highest letter base58flickr Z highest char base58btc z highest char base64 m rfc4648 - no padding base64pad M
    * rfc4648 - with padding - MIME encoding base64url u rfc4648 - no padding base64urlpad U rfc4648 - with padding
    */

  case object Identity extends Base("identity", 0x00, "")
  case object Base1 extends Base("base1", '1', "1")
  case object Base2 extends Base("base2", '0', "01")
  case object Base8 extends Base("base8", '7', "01234567")
  case object Base10 extends Base("base10", '9', "0123456789")

  case object Base16 extends Base16RFC4648("base16", 'f', "0123456789abcdef", None)
  case object Base16Upper extends Base16RFC4648("base16upper", 'F', "0123456789ABCDEF", None)

  case object Base32 extends Base32RFC4648("base32", 'b', "abcdefghijklmnopqrstuvwxyz234567", None)
  case object Base32Upper extends Base32RFC4648("base32upper", 'B', "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567", None)
  case object Base32Pad extends Base32RFC4648("base32pad", 'c', "abcdefghijklmnopqrstuvwxyz234567", Some('='))
  case object Base32PadUpper extends Base32RFC4648("base32padupper", 'C', "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567", Some('='))

  case object Base32Hex extends Base32RFC4648("base32hex", 'v', "0123456789abcdefghijklmnopqrstuv", None)
  case object Base32HexUpper extends Base32RFC4648("base32hexupper", 'V', "0123456789ABCDEFGHIJKLMNOPQRSTUV", None)
  case object Base32HexPad extends Base32RFC4648("base32hexpad", 't', "0123456789abcdefghijklmnopqrstuv", Some('='))
  case object Base32HexPadUpper
      extends Base32RFC4648("base32hexpadupper", 'T', "0123456789ABCDEFGHIJKLMNOPQRSTUV", Some('='))

  case object Base32Z extends Base("base32z", 'h', "ybndrfg8ejkmcpqxot1uwisza345h769")
  case object Base58Flickr
      extends Base("base58flickr", 'Z', "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ")
  case object Base58BTC extends Base("base58btc", 'z', "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")

  case object Base64
      extends Base64RFC4648("base64", 'm', "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", None)
  case object Base64Pad
      extends Base64RFC4648(
        "base64pad",
        'M',
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",
        Some('=')
      )
  case object Base64URL
      extends Base64RFC4648("base64url", 'u', "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_", None)
  case object Base64URLPad
      extends Base64RFC4648(
        "base64urlpad",
        'U',
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_",
        Some('=')
      )

  /** Mappings from Base Code -> Base
    */
  lazy val Codes: Map[Char, Base] = Map(
    Identity.code -> Identity,
    Base1.code -> Base1,
    Base2.code -> Base2,
    Base8.code -> Base8,
    Base10.code -> Base10,
    Base16.code -> Base16,
    Base16Upper.code -> Base16Upper,
    Base32.code -> Base32,
    Base32Upper.code -> Base32Upper,
    Base32Pad.code -> Base32Pad,
    Base32PadUpper.code -> Base32PadUpper,
    Base32Hex.code -> Base32Hex,
    Base32HexUpper.code -> Base32HexUpper,
    Base32HexPad.code -> Base32HexPad,
    Base32HexPadUpper.code -> Base32HexPadUpper,
    Base32Z.code -> Base32Z,
    Base58Flickr.code -> Base58Flickr,
    Base58BTC.code -> Base58BTC,
    Base64.code -> Base64,
    Base64Pad.code -> Base64Pad,
    Base64URL.code -> Base64URL,
    Base64URLPad.code -> Base64URLPad
  )

  /** Mappings from Base Name -> Base
    */
  lazy val Names: Map[String, Base] = Map(
    Identity.name -> Identity,
    Base1.name -> Base1,
    Base2.name -> Base2,
    Base8.name -> Base8,
    Base10.name -> Base10,
    Base16.name -> Base16,
    Base16Upper.name -> Base16Upper,
    Base32.name -> Base32,
    Base32Upper.name -> Base32Upper,
    Base32Pad.name -> Base32Pad,
    Base32PadUpper.name -> Base32PadUpper,
    Base32Hex.name -> Base32Hex,
    Base32HexUpper.name -> Base32HexUpper,
    Base32HexPad.name -> Base32HexPad,
    Base32HexPadUpper.name -> Base32HexPadUpper,
    Base32Z.name -> Base32Z,
    Base58Flickr.name -> Base58Flickr,
    Base58BTC.name -> Base58BTC,
    Base64.name -> Base64,
    Base64Pad.name -> Base64Pad,
    Base64URL.name -> Base64URL,
    Base64URLPad.name -> Base64URLPad
  )

  /** Unsupported Base.
    */
  lazy val Unsupported: Map[Char, Base] = Map(Base1.code -> Base1)

}
