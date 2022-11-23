package fmgp.crypto

import com.nimbusds.jose.UnprotectedHeader //REMOVE
import java.security.MessageDigest
import fmgp.did.VerificationMethodReferenced
import fmgp.util.Base64

case class JWERecipient(vmr: VerificationMethodReferenced, encryptedKey: Base64) {
  // def header = UnprotectedHeader.Builder().keyID(vmr.value).build()
}

object Utils {
  def calculateAPV(refs: Seq[VerificationMethodReferenced]): Base64 = {
    val digest = MessageDigest.getInstance("SHA-256")
    Base64.encode(digest.digest(refs.map(_.value).sorted.mkString(".").getBytes()))
  }

  def calculateAPU(ref: VerificationMethodReferenced): Base64 =
    Base64.encode(ref.value.getBytes())

}
