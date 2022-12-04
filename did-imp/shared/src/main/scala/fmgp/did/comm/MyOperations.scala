package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto.error._
import fmgp.crypto._

class MyOperations extends Operations {

  def sign(msg: PlaintextMessage): ZIO[Agent, CryptoFailed, SignedMessage] =
    for {
      agent <- ZIO.service[Agent]
      key = agent.keys.head // FIXME
      ret <- RawOperations.sign(key, msg)
    } yield ret

  def verify(msg: SignedMessage): ZIO[Resolver, CryptoFailed, Boolean] = {
    for {
      resolver <- ZIO.service[Resolver]
      doc <- resolver
        .didDocument(DIDSubject(msg.signatures.head.header.get.kid))
        .catchAll(error => ???) // FIXME
      key = {
        doc.keyAgreement.get.head match {
          case e: VerificationMethodReferenced        => ??? : PublicKey // FIXME
          case e: VerificationMethodEmbeddedJWK       => e.publicKeyJwk
          case e: VerificationMethodEmbeddedMultibase => ??? : PublicKey // FIXME
        }
      }
      ret <- RawOperations.verify(key, msg)
    } yield ret
  }

  override def anonEncrypt(msg: PlaintextMessage): ZIO[Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      resolver <- ZIO.service[Resolver]
      docs <- ZIO.foreach(msg.to.toSeq.flatten)(resolver.didDocument(_))
      verificationMethods = docs.flatMap(_.didCommKeys)
      recipientKidsKeys = verificationMethods.map {
        case e: VerificationMethodReferenced        => ??? // FIXME
        case e: VerificationMethodEmbeddedJWK       => (VerificationMethodReferenced(e.id), e.publicKeyJwk)
        case e: VerificationMethodEmbeddedMultibase => ??? // FIXME
      } // : Seq[(VerificationMethodReferenced, PublicKey)],
      ret <- RawOperations.anonEncrypt(recipientKidsKeys, msg.toJson.getBytes)
    } yield ret
  }

  def authEncrypt(msg: PlaintextMessage): ZIO[Agent & Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      agent <- ZIO.service[Agent]
      resolver <- ZIO.service[Resolver]
      docs <- ZIO.foreach(msg.to.toSeq.flatten)(resolver.didDocument(_))
      data = msg.toJson.getBytes
      verificationMethods = docs.flatMap(_.didCommKeys)

      senderKeys = agent.keys
        .flatMap(key => key.kid.map(kid => VerificationMethodReferencedWithKey(kid, key)))
        .groupBy(_.key.crv)
        .view
        .mapValues(_.headOption)
        .toMap
      recipientKeys =
        verificationMethods
          .map {
            case e: VerificationMethodReferenced        => ??? // FIXME
            case e: VerificationMethodEmbeddedJWK       => VerificationMethodReferencedWithKey(e.id, e.publicKeyJwk)
            case e: VerificationMethodEmbeddedMultibase => ??? // FIXME
          }
          .groupBy(_.key.crv)
      curve2SenderRecipientKeys = senderKeys
        .map(e => e._1 -> (e._2, recipientKeys.get(e._1).getOrElse(Seq.empty)))
        .toMap
      msgSeq = curve2SenderRecipientKeys.values.toSeq.map {
        case (None, b)                      => ZIO.none
        case (Some(sender), b) if b.isEmpty => ZIO.none
        case (Some(VerificationMethodReferencedWithKey(senderKid, senderKey: ECPrivateKey)), recipientAux) =>
          val recipient = recipientAux.asInstanceOf[Seq[VerificationMethodReferencedWithKey[ECPublicKey]]] // FIXME
          RawOperations.authcryptEC((VerificationMethodReferenced(senderKid), senderKey), recipient, data).map(Some(_))
        case (Some(VerificationMethodReferencedWithKey(senderKid, senderKey: OKPPrivateKey)), recipientAux) =>
          val recipient = recipientAux.asInstanceOf[Seq[VerificationMethodReferencedWithKey[OKPPublicKey]]] // FIXME
          RawOperations.authcryptOKP((VerificationMethodReferenced(senderKid), senderKey), recipient, data).map(Some(_))
      }: Seq[ZIO[Any, CryptoFailed, Option[EncryptedMessage]]]
      ret <- ZIO.foreach(msgSeq)(e => e).map(_.flatten)
    } yield ret.head // FIXME HAED
  }

  /** decrypt */
  def anonDecrypt(msg: EncryptedMessage): ZIO[Agent, DidFail, Message] = {
    for {
      agent <- ZIO.service[Agent]
      did = agent.id
      kidsNeeded = msg.recipients.map(_.header.kid)
      keys = agent.keys
        .flatMap(k =>
          k.kid.flatMap { kid =>
            val vmr = (VerificationMethodReferenced(kid))
            if (kidsNeeded.contains(vmr)) Some(vmr, k) else None
          }
        )
      ret <- RawOperations.anonDecrypt(keys, msg)
    } yield ret
  }

  /** decrypt verify sender */
  def authDecrypt(msg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, Message] =
    for {
      agent <- ZIO.service[Agent]
      did = agent.id
      kidsNeeded = msg.recipients.map(_.header.kid)
      keys = agent.keys
        .flatMap(k =>
          k.kid.flatMap { kid =>
            val vmr = (VerificationMethodReferenced(kid))
            if (kidsNeeded.contains(vmr)) Some(vmr, k) else None
          }
        )
      resolver <- ZIO.service[Resolver]
      skid = msg.`protected`.obj match
        case AnonProtectedHeader(epk, apv, typ, enc, alg)            => ??? // FIXME
        case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) => skid
      senderDID = skid.did
      doc <- resolver.didDocument(senderDID)
      senderKey = doc.didCommKeys
        .map {
          case e: VerificationMethodReferenced        => ??? // FIXME
          case e: VerificationMethodEmbeddedJWK       => VerificationMethodReferencedWithKey(e.id, e.publicKeyJwk)
          case e: VerificationMethodEmbeddedMultibase => ??? // FIXME
        }
        .find { e => e.vmr == skid }
        .get // FIXME
      ret <- RawOperations.authDecrypt(senderKey.key, keys, msg)
    } yield ret

}
