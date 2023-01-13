package fmgp.multibase

opaque type Multibase = String
object Multibase {

  def apply(d: String): Multibase = d

  def encode(base: Base, data: Array[Byte]): Multibase = base.code.toString + {
    base match {
      case Base.Base1             => throw new UnsupportedOperationException("Base1 is not supported yet!")
      case Base.Identity          => IdentityImpl.encode(data)
      case Base.Base16            => Base16Impl.encode(data, Base.Base16)
      case Base.Base16Upper       => Base16Impl.encode(data, Base.Base16Upper)
      case Base.Base32            => Base32Impl.encode(data, Base.Base32)
      case Base.Base32Upper       => Base32Impl.encode(data, Base.Base32Upper)
      case Base.Base32Pad         => Base32Impl.encode(data, Base.Base32Pad)
      case Base.Base32PadUpper    => Base32Impl.encode(data, Base.Base32PadUpper)
      case Base.Base32Hex         => Base32Impl.encode(data, Base.Base32Hex)
      case Base.Base32HexUpper    => Base32Impl.encode(data, Base.Base32HexUpper)
      case Base.Base32HexPad      => Base32Impl.encode(data, Base.Base32HexPad)
      case Base.Base32HexPadUpper => Base32Impl.encode(data, Base.Base32HexPadUpper)
      case Base.Base64            => Base64Impl.encode(data, Base.Base64)
      case Base.Base64Pad         => Base64Impl.encode(data, Base.Base64Pad)
      case Base.Base64URL         => Base64Impl.encode(data, Base.Base64URL)
      case Base.Base64URLPad      => Base64Impl.encode(data, Base.Base64URLPad)
      case _                      => BaseNImpl.encode(base, data)
    }
  }

  def encodeString(base: Base, data: String): Multibase = encode(base, data.getBytes)

  extension (x: Multibase)
    def value: String = x

    def decodeToString: String = new String(x.decode)
    def decodeToHex = x.decode.map("%02X" format _) // .mkString

    def decode: Array[Byte] = {
      val baseOpt = Base.Codes.get(x.charAt(0))
      if (baseOpt.isEmpty) { // TODO REMOVE ERROR
        throw new IllegalArgumentException("Cannot get Multibase type from input data: " + x)
      }

      val base = baseOpt.get
      val rest = x.substring(1)
      if (rest.isEmpty) Array[Byte]()
      else
        base match {
          case Base.Base1             => throw new UnsupportedOperationException("Base1 is not supported yet!")
          case Base.Identity          => IdentityImpl.decode(rest)
          case Base.Base16            => Base16Impl.decode(rest, Base.Base16)
          case Base.Base16Upper       => Base16Impl.decode(rest, Base.Base16Upper)
          case Base.Base32            => Base32Impl.decode(rest, Base.Base32)
          case Base.Base32Upper       => Base32Impl.decode(rest, Base.Base32Upper)
          case Base.Base32Pad         => Base32Impl.decode(rest, Base.Base32Pad)
          case Base.Base32PadUpper    => Base32Impl.decode(rest, Base.Base32PadUpper)
          case Base.Base32Hex         => Base32Impl.decode(rest, Base.Base32Hex)
          case Base.Base32HexUpper    => Base32Impl.decode(rest, Base.Base32HexUpper)
          case Base.Base32HexPad      => Base32Impl.decode(rest, Base.Base32HexPad)
          case Base.Base32HexPadUpper => Base32Impl.decode(rest, Base.Base32HexPadUpper)
          case Base.Base64            => Base64Impl.decode(rest, Base.Base64)
          case Base.Base64Pad         => Base64Impl.decode(rest, Base.Base64Pad)
          case Base.Base64URL         => Base64Impl.decode(rest, Base.Base64URL)
          case Base.Base64URLPad      => Base64Impl.decode(rest, Base.Base64URLPad)
          case _                      => BaseNImpl.decode(base, rest)
        }
    }.dropWhile(b => b == 0)

}
