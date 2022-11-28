package fmgp.crypto

import java.security.MessageDigest

object SHA256 {
  def digest(data: Array[Byte]): Array[Byte] = MessageDigest
    .getInstance("SHA-256")
    .digest(data)
}
