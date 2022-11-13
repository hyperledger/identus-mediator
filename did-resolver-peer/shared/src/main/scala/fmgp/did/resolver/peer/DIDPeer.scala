package fmgp.did.resolver.peer

import zio._
import fmgp.did._
import fmgp.multibase._
import fmgp.crypto.error.DidMethodNotSupported
import fmgp.crypto.OKPPublicKey
import fmgp.crypto.KTY
import fmgp.crypto.Curve
import zio.json._
import java.util.Base64
import javax.lang.model.element.Element

/** DID Peer
  *
  * @see
  *   https://identity.foundation/peer-did-method-spec/#method-specific-identifier
  */
sealed trait DIDPeer extends DID {
  def namespace: String = "peer"
  def specificId: String
  def numalgo: 0 | 1 | 2

  def document: DIDDocument
}

case class DIDPeer0(encnumbasis: DIDPeer.Array46_BASE58BTC) extends DIDPeer {
  def numalgo: 0 = 0
  def specificId: String = "" + numalgo + encnumbasis
  override def document: DIDDocument = ??? // FIXME
}
case class DIDPeer1(encnumbasis: DIDPeer.Array46_BASE58BTC) extends DIDPeer {
  def numalgo: 1 = 1
  def specificId: String = "" + numalgo + encnumbasis
  override def document: DIDDocument = ??? // FIXME
}
case class DIDPeer2(elements: Seq[DIDPeer2.Element]) extends DIDPeer {
  def numalgo: 2 = 2
  def specificId: String = "" + numalgo + elements.map(_.encode).mkString(".", ".", "")

  override def document: DIDDocument = DIDDocumentClass(
    id = this,
    authentication = Some(
      elements
        .collect { case e: DIDPeer2.ElementV =>
          VerificationMethodEmbeddedJWK(
            id = this.string + "#" + e.mb.value.drop(1),
            controller = this,
            `type` = "JsonWebKey2020",
            publicKeyJwk = OKPPublicKey(
              kty = KTY.OKP,
              crv = Curve.Ed25519,
              x = DIDPeer.decodeKey(e.mb.value),
              kid = None // Option[String]
            )
          )
        }
    ),
    keyAgreement = Some(
      elements.collect { case e: DIDPeer2.ElementE =>
        VerificationMethodEmbeddedJWK(
          id = this.string + "#" + e.mb.value.drop(1),
          controller = this,
          `type` = "JsonWebKey2020",
          publicKeyJwk = OKPPublicKey(
            kty = KTY.OKP,
            crv = Curve.X25519,
            x = DIDPeer.decodeKey(e.mb.value),
            kid = None // Option[String]
          )
        )
      }.toSet
    ),
    service = Some(
      elements
        .collect { case e: DIDPeer2.ElementService =>
          val data = String(Base64.getDecoder().decode(e.base64))
          data
            .fromJson[DIDPeerServiceEncoded]
            .toOption // TODO deal with errors
            .map { x => x.getDIDService(DIDSubject(this.string)) }
        }
        .flatten
        .toSet
    ).filterNot(_.isEmpty)
  )
}
object DIDPeer2 {
  // type Purposecode = 'A' | 'E' | 'V' | 'I' | 'D' | 'S'
  sealed trait Element { def encode: String } // TODO

  /** A - keyAgreement */
  case class ElementA(mb: Multibase) extends Element { def encode = "A" + mb }
  case class ElementE(mb: Multibase) extends Element { def encode = "E" + mb }

  /** V - authentication key */
  case class ElementV(mb: Multibase) extends Element { def encode = "V" + mb }
  case class ElementI(mb: Multibase) extends Element { def encode = "I" + mb }
  case class ElementD(mb: Multibase) extends Element { def encode = "D" + mb }
  case class ElementService(base64: C1_B64URL) extends Element { def encode = "S" + base64 } // TODO
  // case class Element(code: Purposecode, value: String) { def encode = "" + code + value }
  type C1_B64URL = String
}

object DIDPeer {
  type Array46_BASE58BTC = String // TODO  "46*BASE58BTC"

  def transform = 'z'

  def regexPeer =
    "^did:peer:(([01](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))|(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)?)))$".r
  def regexPeer0 = "^did:peer:(0(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  def regexPeer1 = "^did:peer:(1(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  // #regexPeer2 = "^did:peer:(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)?))$".r
  def regexPeer2 = "^did:peer:2((\\.([AEVID])z([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)([0-9a-zA-Z=]*))?)$".r

  def apply(didSubject: DIDSubject): DIDPeer = asDIDPeer(didSubject.toDID)

  def asDIDPeer(did: DID) = did.string match {
    case regexPeer0(all, z: "z", data) => DIDPeer0(all.drop(1))
    case regexPeer1(all, z: "z", data) => DIDPeer1(all.drop(1))
    case regexPeer2(all, str: _*) =>
      val elements = all
        .drop(1) // drop peer type number
        .split('.')
        .toSeq
        .map {
          case s if s.startsWith("A") => DIDPeer2.ElementA(Multibase(s.drop(1)))
          case s if s.startsWith("E") => DIDPeer2.ElementE(Multibase(s.drop(1)))
          case s if s.startsWith("V") => DIDPeer2.ElementV(Multibase(s.drop(1)))
          case s if s.startsWith("I") => DIDPeer2.ElementI(Multibase(s.drop(1)))
          case s if s.startsWith("D") => DIDPeer2.ElementD(Multibase(s.drop(1)))
          case s if s.startsWith("S") => DIDPeer2.ElementService(s.drop(1)) // Base64
        }
      DIDPeer2(elements)
    case any if regexPeer.matches(any) => ??? // TODO
  }

  def decodeKey(data: String): String = {
    import fmgp.multibase.Base
    import fmgp.multibase.Base64Impl
    import fmgp.multibase.Multibase
    val tmp = Multibase(data).decode.drop(2) // multibase type
    Multibase.encode(Base.Base64URL, tmp).value.drop(1)
  }

}
