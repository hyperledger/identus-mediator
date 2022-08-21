package fmgp.crypto

import com.nimbusds.jose.util.Base64URL
import java.security.MessageDigest
import fmgp.did.VerificationMethodReferenced

object Utils {
  def calculateAPV(refs: Seq[VerificationMethodReferenced]): Base64URL = {
    val digest = MessageDigest.getInstance("SHA-256")
    Base64URL.encode(digest.digest(refs.map(_.value).sorted.mkString(".").getBytes()))
  }

  def calculateAPU(ref: VerificationMethodReferenced): Base64URL =
    Base64URL.encode(ref.value.getBytes())

}
