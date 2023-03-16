package fmgp.did

import zio.json._
import fmgp.crypto._
import fmgp.did._

object DidExample {

  // ##################
  // ### Sender Key ###
  // ##################

  /** key-1 */
  def senderKeyEd25519 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:alice#key-1"),
    crv = Curve.Ed25519,
    d = "pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
    x = "G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww"
  )

  /** key-2 */
  def senderKeyP256_1 =
    ECPrivateKey(
      kty = KTY.EC,
      kid = Some("did:example:alice#key-2"),
      crv = Curve.`P-256`,
      d = "7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A",
      x = "2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY",
      y = "BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w"
    )

  /** key-3 */
  def senderKeySecp256k1 = ECPrivateKey(
    kty = KTY.EC,
    crv = Curve.secp256k1,
    kid = Some("did:example:alice#key-3"),
    d = "N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA",
    x = "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
    y = "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
  )

  /** key-4 */
  def senderKeyX25519 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:alice#key-x25519-1"),
    crv = Curve.X25519,
    d = "r-jK2cO3taR8LQnJB1_ikLBTAnOtShJOsHXRUWT-aZA",
    x = "avH0O2Y4tqLAq8y9zpianr8ajii5m4F_mICrzNlatXs"
  )

  /** key-5 */
  def senderKeyP256_2 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:alice#key-p256-1"),
    crv = Curve.`P-256`,
    d = "sB0bYtpaXyp-h17dDpMx91N3Du1AdN4z1FUq02GbmLw",
    x = "L0crjMN1g0Ih4sYAJ_nGoHUck2cloltUpUVQDhF2nHE",
    y = "SxYgE7CmEJYi7IDhgK5jI4ZiajO8jPRZDldVhqFpYoo"
  )

  /** key-6 */
  def senderKeyP521 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:alice#key-p521-1"),
    crv = Curve.`P-521`,
    d = "AQCQKE7rZpxPnX9RgjXxeywrAMp1fJsyFe4cir1gWj-8t8xWaM_E2qBkTTzyjbRBu-JPXHe_auT850iYmE34SkWi",
    x = "AHBEVPRhAv-WHDEvxVM9S0px9WxxwHL641Pemgk9sDdxvli9VpKCBdra5gg_4kupBDhz__AlaBgKOC_15J2Byptz",
    y = "AciGcHJCD_yMikQvlmqpkBbVqqbg93mMVcgvXBYAQPP-u9AF7adybwZrNfHWCKAQwGF9ugd0Zhg7mLMEszIONFRk"
  )

  // #####################
  // ### Recipient Key ###
  // #####################

  def recipientKey1 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:bob#key-x25519-1"),
    crv = Curve.X25519,
    d = "b9NnuOCB0hm7YGNvaE9DMhwH_wjZA1-gWD6dA0JWdL0",
    x = "GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
  )
  def recipientKey2 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:bob#key-x25519-2"),
    crv = Curve.X25519,
    d = "p-vteoF1gopny1HXywt76xz_uC83UUmrgszsI-ThBKk",
    x = "UT9S3F5ep16KSNBBShU2wh3qSfqYjlasZimn0mB8_VM"
  )
  def recipientKey3 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:bob#key-x25519-3"),
    crv = Curve.X25519,
    d = "f9WJeuQXEItkGM8shN4dqFr5fLQLBasHnWZ-8dPaSo0",
    x = "82k2BTUiywKv49fKLZa-WwDi8RBf0tB0M8bvSAUQ3yY"
  )

  def recipientKey4 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p256-1"),
    crv = Curve.`P-256`,
    d = "PgwHnlXxt8pwR6OCTUwwWx-P51BiLkFZyqHzquKddXQ",
    x = "FQVaTOksf-XsCUrt4J1L2UGvtWaDwpboVlqbKBY2AIo",
    y = "6XFB9PYo7dyC5ViJSO9uXNYkxTJWn0d_mqJ__ZYhcNY"
  )
  def recipientKey5 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p256-2"),
    crv = Curve.`P-256`,
    d = "agKz7HS8mIwqO40Q2dwm_Zi70IdYFtonN5sZecQoxYU",
    x = "n0yBsGrwGZup9ywKhzD4KoORGicilzIUyfcXb1CSwe0",
    y = "ov0buZJ8GHzV128jmCw1CaFbajZoFFmiJDbMrceCXIw"
  )
  def recipientKey6 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p384-1"),
    crv = Curve.`P-384`,
    d = "ajqcWbYA0UDBKfAhkSkeiVjMMt8l-5rcknvEv9t_Os6M8s-HisdywvNCX4CGd_xY",
    x = "MvnE_OwKoTcJVfHyTX-DLSRhhNwlu5LNoQ5UWD9Jmgtdxp_kpjsMuTTBnxg5RF_Y",
    y = "X_3HJBcKFQEG35PZbEOBn8u9_z8V1F9V1Kv-Vh0aSzmH-y9aOuDJUE3D4Hvmi5l7"
  )
  def recipientKey7 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p384-2"),
    crv = Curve.`P-384`,
    d = "OiwhRotK188BtbQy0XBO8PljSKYI6CCD-nE_ZUzK7o81tk3imDOuQ-jrSWaIkI-T",
    x = "2x3HOTvR8e-Tu6U4UqMd1wUWsNXMD0RgIunZTMcZsS-zWOwDgsrhYVHmv3k_DjV3",
    y = "W9LLaBjlWYcXUxOf6ECSfcXKaC3-K9z4hCoP0PS87Q_4ExMgIwxVCXUEB6nf0GDd"
  )
  def recipientKey8 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p521-1"),
    crv = Curve.`P-521`,
    d = "AV5ocjvy7PkPgNrSuvCxtG70NMj6iTabvvjSLbsdd8OdI9HlXYlFR7RdBbgLUTruvaIRhjEAE9gNTH6rWUIdfuj6",
    x = "Af9O5THFENlqQbh2Ehipt1Yf4gAd9RCa3QzPktfcgUIFADMc4kAaYVViTaDOuvVS2vMS1KZe0D5kXedSXPQ3QbHi",
    y = "ATZVigRQ7UdGsQ9j-omyff6JIeeUv3CBWYsZ0l6x3C_SYqhqVV7dEG-TafCCNiIxs8qeUiXQ8cHWVclqkH4Lo1qH"
  )
  def recipientKey9 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p521-2"),
    crv = Curve.`P-521`,
    d = "ABixMEZHsyT7SRw-lY5HxdNOofTZLlwBHwPEJ3spEMC2sWN1RZQylZuvoyOBGJnPxg4-H_iVhNWf_OtgYODrYhCk",
    x = "ATp_WxCfIK_SriBoStmA0QrJc2pUR1djpen0VdpmogtnKxJbitiPq-HJXYXDKriXfVnkrl2i952MsIOMfD2j0Ots",
    y = "AEJipR0Dc-aBZYDqN51SKHYSWs9hM58SmRY1MxgXANgZrPaq1EeGMGOjkbLMEJtBThdjXhkS5VlXMkF0cYhZELiH"
  )

  // #######################################################

  val senderSecrets = KeyStore(
    Set(
      senderKeyEd25519,
      senderKeyP256_1,
      senderKeySecp256k1,
      senderKeyX25519,
      senderKeyP256_2,
      senderKeyP521,
    )
  )

  val recipientSecrets = KeyStore(
    Set(
      recipientKey1,
      recipientKey2,
      recipientKey3,
      recipientKey4,
      recipientKey5,
      recipientKey6,
      recipientKey7,
      recipientKey8,
      recipientKey9,
    )
  )

  val senderDIDDocument =
    DIDDocumentClass(
      // "@context":["https://www.w3.org/ns/did/v1","https://w3id.org/security/suites/jws-2020/v1"],
      id = DIDSubject("did:example:alice"),
      authentication = Some(
        Seq(
          VerificationMethodEmbeddedJWK(
            id = "did:example:alice#key-1",
            controller = "did:example:alice#key-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = senderKeyEd25519.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:alice#key-2",
            controller = "did:example:alice#key-2",
            `type` = "JsonWebKey2020",
            publicKeyJwk = senderKeyP256_1.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:alice#key-3",
            controller = "did:example:alice#key-3",
            `type` = "JsonWebKey2020",
            publicKeyJwk = senderKeySecp256k1.toPublicKey.copy(kid = None)
          ),
        )
      ),
      keyAgreement = Some(
        Set(
          VerificationMethodEmbeddedJWK(
            id = "did:example:alice#key-x25519-1",
            controller = "did:example:alice#key-x25519-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = senderKeyX25519.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:alice#key-p256-1",
            controller = "did:example:alice#key-p256-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = senderKeyP256_2.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:alice#key-p521-1",
            controller = "did:example:alice#key-p521-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = senderKeyP521.toPublicKey.copy(kid = None)
          ),
        )
      )
    )

  val recipientDIDDocument =
    DIDDocumentClass(
      // "@context":["https://www.w3.org/ns/did/v2"],
      id = DIDSubject("did:example:bob"),
      keyAgreement = Some(
        Set(
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-x25519-1",
            controller = "did:example:bob#key-x25519-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey1.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-x25519-2",
            controller = "did:example:bob#key-x25519-2",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey2.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-x25519-3",
            controller = "did:example:bob#key-x25519-3",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey3.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-p256-1",
            controller = "did:example:bob#key-p256-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey4.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-p256-2",
            controller = "did:example:bob#key-p256-2",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey5.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-p384-1",
            controller = "did:example:bob#key-p384-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey6.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-p384-2",
            controller = "did:example:bob#key-p384-2",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey7.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-p521-1",
            controller = "did:example:bob#key-p521-1",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey8.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = "did:example:bob#key-p521-2",
            controller = "did:example:bob#key-p521-2",
            `type` = "JsonWebKey2020",
            publicKeyJwk = recipientKey9.toPublicKey.copy(kid = None)
          ),
        )
      )
    )

}
