package fmgp.util

import java.{util => ju}

// Base64 URL
opaque type Base64 = Array[Byte]

object Base64:
  /** @param str
    *   Base64 URL string
    */
  def apply(str: String): Base64 = str.getBytes

  /** @param str
    *   Base64 URL Byte
    */
  def apply(bytes: Array[Byte]): Base64 = bytes
  def fromBase64url(str: String): Base64 = str.getBytes // ju.Base64.getUrlDecoder.decode(str.getBytes)
  def fromBase64(str: String): Base64 = encode(ju.Base64.getDecoder.decode(str.getBytes))
  def encode(str: String): Base64 = ju.Base64.getUrlEncoder.encode(str.getBytes)
  def encode(data: Array[Byte]): Base64 = ju.Base64.getUrlEncoder.encode(data)

extension (bytes: Base64)
  def url: String = String(bytes).filterNot(_ == '=')
  def base64: String = String(
    ju.Base64.getEncoder.encode {
      ju.Base64.getUrlDecoder().decode(bytes)
    }
  )
  def bytes: Array[Byte] = bytes
  def decode: String = String(ju.Base64.getUrlDecoder.decode(bytes))
