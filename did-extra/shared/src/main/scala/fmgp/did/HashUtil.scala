package fmgp.did

import zio.json._
import zio.prelude.Equal
import zio.prelude.Hash

import fmgp.did._
import fmgp.did.comm._
import fmgp.util._

//Maybe this is a bit offer power

given hashTag: Hash[TAG] = Hash[String].contramap[TAG](_.value)
given hashIV: Hash[IV] = Hash[String].contramap[IV](_.value)

given hashSeqByte: Hash[Seq[Byte]] = Hash.ListHash[Byte].contramap[Seq[Byte]](_.toList)
given hashSeqRecipient: Hash[Seq[Recipient]] = Hash.ListHash[Recipient].contramap[Seq[Recipient]](_.toList)

given hashBase64: Hash[Base64] = Hash[Seq[Byte]].contramap[Base64](_.bytes.toSeq)
given hashCipherText: Hash[CipherText] = Hash[Base64].contramap[CipherText](_.base64)
given hashBase64Obj: Hash[Base64Obj[ProtectedHeader]] =
  Hash[Base64].contramap[Base64Obj[ProtectedHeader]](o =>
    o.original match
      case Some(value) => value
      case None        => Base64.fromBase64url(o.obj.toJson)
  )

// hashBase64.asInstanceOf[Hash[CipherText]]

given hashVerificationMethodReferenced: Hash[VerificationMethodReferenced] =
  Hash[String].contramap[VerificationMethodReferenced](_.value)

given hashRecipientHeader: Hash[RecipientHeader] =
  Hash[VerificationMethodReferenced].contramap[RecipientHeader](_.kid)

given hashRecipients: Hash[Recipient] =
  Hash[(Base64, RecipientHeader)].contramap[Recipient](e => (e.encrypted_key, e.header))

given hashEncryptedMessage: Hash[EncryptedMessage] =
  Hash[
    (
        CipherText,
        Base64Obj[ProtectedHeader],
        Seq[Recipient],
        TAG,
        IV
    )
  ].contramap[EncryptedMessage](obj => (obj.ciphertext, obj.`protected`, obj.recipients, obj.tag, obj.iv))
