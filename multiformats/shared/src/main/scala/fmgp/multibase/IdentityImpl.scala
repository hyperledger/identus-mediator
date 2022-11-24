package fmgp.multibase

object IdentityImpl {

  def encode(data: Array[Byte]): String = new String(data)

  def decode(data: String): Array[Byte] = data.getBytes()

}
