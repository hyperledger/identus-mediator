package fmgp.crypto

import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.UnprotectedHeader //REMOVE
import java.security.MessageDigest
import fmgp.did.VerificationMethodReferenced

case class JWERecipient(vmr: VerificationMethodReferenced, encryptedKey: Base64URL) {
  // def header = UnprotectedHeader.Builder().keyID(vmr.value).build()
}

object Utils {
  def calculateAPV(refs: Seq[VerificationMethodReferenced]): Base64URL = {
    val digest = MessageDigest.getInstance("SHA-256")
    Base64URL.encode(digest.digest(refs.map(_.value).sorted.mkString(".").getBytes()))
  }

  def calculateAPU(ref: VerificationMethodReferenced): Base64URL =
    Base64URL.encode(ref.value.getBytes())

}
