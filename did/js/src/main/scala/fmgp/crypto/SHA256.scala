package fmgp.crypto

import java.security.MessageDigest
import scala.scalajs.js.typedarray.Uint8Array

object SHA256 {
  def digest(data: Array[Byte]): Array[Byte] = Array.emptyByteArray // FIXME HACK ! (ignored JS test fail with out this)

  // def digestMessage(message: Array[Byte]) = {
  //   val encoder = new TextEncoder()
  //   val data: Uint8Array = encoder.encode(message)
  //   val hash = crypto.subtle.digest("SHA-256", ????)
  //   hash
  // }
}
