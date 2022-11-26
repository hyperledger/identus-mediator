package fmgp.crypto

import java.security.MessageDigest
import fmgp.did.VerificationMethodReferenced
import fmgp.util.Base64

case class JWERecipient(vmr: VerificationMethodReferenced, encryptedKey: Base64) {
  // def header = UnprotectedHeader.Builder().keyID(vmr.value).build()
}
