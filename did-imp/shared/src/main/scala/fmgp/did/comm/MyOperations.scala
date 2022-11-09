package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto.error._
import fmgp.crypto._


class MyOperations extends Operations {

  def sign(msg: PlaintextMessageClass): ZIO[Agent, CryptoFailed, SignedMessage] =
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

  override def anonEncrypt(msg: PlaintextMessageClass): ZIO[Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      resolver <- ZIO.service[Resolver]
      docs <- ZIO.foreach(msg.to.toSeq.flatten)(e => resolver.didDocument(DIDSubject(e)))
      verificationMethods = docs.flatMap(_.verificationMethod.toSeq.flatten)
      recipientKidsKeys = verificationMethods.map {
        case e: VerificationMethodReferenced        => ??? // FIXME
        case e: VerificationMethodEmbeddedJWK       => (VerificationMethodReferenced(e.id), e.publicKeyJwk)
        case e: VerificationMethodEmbeddedMultibase => ??? // FIXME
      } // : Seq[(VerificationMethodReferenced, PublicKey)],
      ret <- RawOperations.anonEncrypt(recipientKidsKeys, msg.toJson.getBytes)
    } yield ret
  }

  def authEncrypt(msg: PlaintextMessageClass): ZIO[Agent & Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      agent <- ZIO.service[Agent]
      senderKidKey = agent.keys
        .flatMap(e => e.kid.map(kid => (VerificationMethodReferenced(kid), e)))
        .head // FIXME
      resolver <- ZIO.service[Resolver]
      docs <- ZIO.foreach(msg.to.toSeq.flatten)(e => resolver.didDocument(DIDSubject(e)))
      verificationMethods = docs.flatMap(_.verificationMethod.toSeq.flatten)
      recipientKidsKeys = verificationMethods.map {
        case e: VerificationMethodReferenced        => ??? // FIXME
        case e: VerificationMethodEmbeddedJWK       => (VerificationMethodReferenced(e.id), e.publicKeyJwk)
        case e: VerificationMethodEmbeddedMultibase => ??? // FIXME
      } // : Seq[(VerificationMethodReferenced, PublicKey)],
      ret <- RawOperations.authEncrypt(senderKidKey, recipientKidsKeys, msg.toJson.getBytes)
    } yield ret
  }

  /** decrypt */
  def anonDecrypt(msg: EncryptedMessage): ZIO[Agent & Resolver, CryptoFailed, Message] =
    ???

  /** decrypt verify sender */
  def authDecrypt(msg: EncryptedMessage): ZIO[Agent & Resolver, CryptoFailed, Message] =
    ???

}
