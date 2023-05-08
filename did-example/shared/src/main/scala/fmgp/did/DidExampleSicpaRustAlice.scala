package fmgp.did

import zio.json._
import fmgp.crypto._
import fmgp.did._

object DidExampleSicpaRustAlice {

  // #################################
  // ### did:example:alice Key ###
  // #################################

  def aliceKeyX25519NotInSecrets = OKPPublicKey(
    kty = KTY.OKP,
    kid = Some("did:example:alice#key-x25519-not-in-secrets-1"),
    crv = Curve.X25519,
    x = "avH0O2Y4tqLAq8y9zpianr8ajii5m4F_mICrzNlatXs"
  )
  def aliceKeyX25519 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:alice#key-x25519-1"),
    crv = Curve.X25519,
    d = "r-jK2cO3taR8LQnJB1_ikLBTAnOtShJOsHXRUWT-aZA",
    x = "avH0O2Y4tqLAq8y9zpianr8ajii5m4F_mICrzNlatXs"
  )
  def aliceKeyP256 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:alice#key-p256-1"),
    crv = Curve.`P-256`,
    d = "sB0bYtpaXyp-h17dDpMx91N3Du1AdN4z1FUq02GbmLw",
    x = "L0crjMN1g0Ih4sYAJ_nGoHUck2cloltUpUVQDhF2nHE",
    y = "SxYgE7CmEJYi7IDhgK5jI4ZiajO8jPRZDldVhqFpYoo"
  )
  def aliceKeyP521 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:alice#key-p521-1"),
    crv = Curve.`P-521`,
    d = "AQCQKE7rZpxPnX9RgjXxeywrAMp1fJsyFe4cir1gWj-8t8xWaM_E2qBkTTzyjbRBu-JPXHe_auT850iYmE34SkWi",
    x = "AHBEVPRhAv-WHDEvxVM9S0px9WxxwHL641Pemgk9sDdxvli9VpKCBdra5gg_4kupBDhz__AlaBgKOC_15J2Byptz",
    y = "AciGcHJCD_yMikQvlmqpkBbVqqbg93mMVcgvXBYAQPP-u9AF7adybwZrNfHWCKAQwGF9ugd0Zhg7mLMEszIONFRk"
  )
  def aliceKey1NotInSecrets = OKPPublicKey(
    kty = KTY.OKP,
    kid = Some("did:example:alice#key-not-in-secrets-1"),
    crv = Curve.Ed25519,
    x = "G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww",
  )
  def aliceKey1 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:alice#key-1"),
    crv = Curve.Ed25519,
    d = "pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
    x = "G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww",
  )
  def aliceKey2 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:alice#key-2"),
    crv = Curve.`P-256`,
    d = "7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A",
    x = "2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY",
    y = "BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w"
  )
  def aliceKey3 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:alice#key-3"),
    crv = Curve.secp256k1,
    d = "N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA",
    x = "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
    y = "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
  )

  // // #######################################################

  val aliceSecrets = KeyStore(
    Set(
      aliceKeyX25519,
      aliceKeyP256,
      aliceKeyP521,
      aliceKey1,
      aliceKey2,
      aliceKey3,
    )
  )

  val aliceDIDDocument =
    DIDDocumentClass(
      // "@context":["https://www.w3.org/ns/did/v2"],
      id = DIDSubject("did:example:alice"),
      authentication = Some(
        Seq(
          VerificationMethodReferenced("did:example:alice#key-not-in-secrets-1"),
          VerificationMethodReferenced("did:example:alice#key-1"),
          VerificationMethodReferenced("did:example:alice#key-2"),
          VerificationMethodReferenced("did:example:alice#key-3"),
        )
      ),
      keyAgreement = Some(
        Set(
          VerificationMethodReferenced("did:example:alice#key-x25519-not-in-secrets-1"),
          VerificationMethodReferenced("did:example:alice#key-x25519-1"),
          VerificationMethodReferenced("did:example:alice#key-p256-1"),
          VerificationMethodReferenced("did:example:alice#key-p521-1"),
        )
      ),
      verificationMethod = Some(
        Set(
          VerificationMethodEmbeddedJWK(
            id = aliceKeyX25519NotInSecrets.kid.get,
            controller = aliceKeyX25519NotInSecrets.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKeyX25519NotInSecrets.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = aliceKeyX25519.kid.get,
            controller = aliceKeyX25519.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKeyX25519.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = aliceKeyP256.kid.get,
            controller = aliceKeyP256.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKeyP256.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = aliceKeyP521.kid.get,
            controller = aliceKeyP521.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKeyP521.toPublicKey.copy(kid = None)
          ),
          //
          VerificationMethodEmbeddedJWK(
            id = aliceKey1NotInSecrets.kid.get,
            controller = aliceKey1NotInSecrets.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKey1NotInSecrets.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = aliceKey1.kid.get,
            controller = aliceKey1.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKey1.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = aliceKey2.kid.get,
            controller = aliceKey2.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKey2.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = aliceKey3.kid.get,
            controller = aliceKey3.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = aliceKey3.toPublicKey.copy(kid = None)
          ),
        )
      )
    )

}
