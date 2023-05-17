package fmgp.did.method.peer

import zio._
import fmgp.did._
import fmgp.util._
import fmgp.multibase._
import fmgp.crypto._
import fmgp.crypto.error.DidMethodNotSupported
import zio.json._
import javax.lang.model.element.Element
import fmgp.multibase.Base.Base58BTC

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
            controller = this.did,
            `type` = "JsonWebKey2020",
            publicKeyJwk = OKPPublicKey(
              kty = KTY.OKP,
              crv = Curve.Ed25519, // TODO PLZ FIX BLACKMAGIC of did:peer! (zero documentation & unexpected logic)
              x = DIDPeer.decodeKey(e.mb.value),
              kid = None
            )
          )
        }
    ),
    keyAgreement = Some(
      elements.collect { case e: DIDPeer2.ElementE =>
        VerificationMethodEmbeddedJWK(
          id = this.string + "#" + e.mb.value.drop(1),
          controller = this.did,
          `type` = "JsonWebKey2020",
          publicKeyJwk = OKPPublicKey(
            kty = KTY.OKP,
            crv = Curve.X25519, // TODO PLZ FIX BLACKMAGIC of did:peer! (zero documentation & unexpected logic)
            x = DIDPeer.decodeKey(e.mb.value),
            kid = None
          )
        )
      }.toSet
    ),
    service = Some(
      elements
        .collect { case e: DIDPeer2.ElementService =>
          val data = String(Base64.basicDecoder.decode(e.base64))
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

  type C1_B64URL = String
  case class ElementService(base64: C1_B64URL) extends Element { def encode = "S" + base64 }

  object ElementService {
    def apply(obj: DIDPeerServiceEncoded): ElementService = new ElementService(Base64.encode(obj.toJson).urlBase64)
  }

  // case class Element(code: Purposecode, value: String) { def encode = "" + code + value }

  def apply(keys: Seq[PrivateKey], service: Seq[DIDPeerServiceEncoded] = Seq.empty): DIDPeer2 =
    DIDPeer2(keys.map(keyToElement(_)) ++ service.map(ElementService(_)))

  def makeAgent(keySeq: Seq[PrivateKey], service: Seq[DIDPeerServiceEncoded] = Seq.empty): DIDPeer.AgentDIDPeer =
    new DIDPeer.AgentDIDPeer {
      val id: DIDPeer2 = apply(keySeq, service)
      val keys: Seq[PrivateKey] = keySeq.map(k => keyKidAbsolute(k, id))
    }

  def keyToElement(key: PrivateKey) = key match {
    case key @ OKPPrivateKey(kty, Curve.X25519, d, x, kid) =>
      DIDPeer2.ElementE(
        Multibase.encode(
          Base58BTC,
          Array(-20.toByte, 1.toByte).map(_.toByte) ++ Base64(key.x).decode // TODO refactoring
        )
      )
    case key @ OKPPrivateKey(kty, Curve.Ed25519, d, x, kid) =>
      DIDPeer2.ElementV(
        Multibase.encode(
          Base58BTC,
          Array(-19.toByte, 1.toByte).map(_.toByte) ++ Base64(key.x).decode // TODO refactoring
        )
      )
    case key @ OKPPrivateKey(kty, crv, d, x, kid) => ??? // ERROR!
    case ECPrivateKey(kty, crv, d, x, y, kid)     => ??? // TODO
  }

  def keyKidAbsolute(key: PrivateKey, did: DIDPeer) = key match
    case k @ OKPPrivateKey(kty, crv, d, x, kid) =>
      k.copy(kid = Some(did.did + "#" + keyToElement(k).encode.drop(2))) // FIXME .drop(2) 'Sz'
    case k @ ECPrivateKey(kty, crv, d, x, y, kid) =>
      k.copy(kid = Some(did.did + "#" + keyToElement(k).encode.drop(2))) // FIXME .drop(2) 'Sz'

  def keyKidRelative(key: PrivateKey) = key match
    case k @ OKPPrivateKey(kty, crv, d, x, kid) =>
      k.copy(kid = Some(keyToElement(k).encode.drop(2))) // FIXME .drop(2) 'Sz'
    case k @ ECPrivateKey(kty, crv, d, x, y, kid) =>
      k.copy(kid = Some(keyToElement(k).encode.drop(2))) // FIXME .drop(2) 'Sz'

  def fromDID(did: DID): Either[String, DIDPeer2] = did.string match {
    case DIDPeer.regexPeer2(all, str: _*) =>
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
      Right(DIDPeer2(elements))
    case any if DIDPeer.regexPeer.matches(any) => Left(s"Not a did:peer:2... '$any'") // FIXME make Error type
  }

}

object DIDPeer {
  type Array46_BASE58BTC = String // TODO  "46*BASE58BTC"

  trait AgentDIDPeer extends Agent {
    override def id: DIDPeer
  }

  def regexPeer =
    "^did:peer:(([01](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))|(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)?)))$".r
  def regexPeer0 = "^did:peer:(0(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  def regexPeer1 = "^did:peer:(1(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  // #regexPeer2 = "^did:peer:(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)?))$".r
  def regexPeer2 = "^did:peer:2((\\.([AEVID])z([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)([0-9a-zA-Z=]*))?)$".r

  def apply(didSubject: DIDSubject): DIDPeer = fromDID(didSubject.toDID).toOption.get // FIXME!

  def fromDID(did: DID): Either[String, DIDPeer] = did.string match {
    case regexPeer0(all, z: "z", data) => Right(DIDPeer0(all.drop(1)))
    case regexPeer1(all, z: "z", data) => Right(DIDPeer1(all.drop(1)))
    case regexPeer2(all, str: _*)      => DIDPeer2.fromDID(did)
    case any if regexPeer.matches(any) => Left(s"Not a did:peer '$any'") // FIXME make Error type
  }

  def decodeKey(data: String): String = {
    val tmp = Multibase(data).decode
      .drop(1) // FIXME drop 1 for the multicodec type (0x12) and another becuase the type the multihash size
      .drop(1) // FIXME drop 1 another becuase the type is multihash and this byte (0x20) is for the size
    Base64.encode(tmp).urlBase64
  }

}
