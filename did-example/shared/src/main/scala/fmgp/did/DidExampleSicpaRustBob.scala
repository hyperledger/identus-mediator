package fmgp.did

import zio.json._
import fmgp.crypto._
import fmgp.did._
import zio.json.ast._

object DidExampleSicpaRustBob {

  // ###########################
  // ### did:example:bob Key ###
  // ###########################

  def bobKeyX25519_1 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:bob#key-x25519-1"),
    crv = Curve.X25519,
    d = "b9NnuOCB0hm7YGNvaE9DMhwH_wjZA1-gWD6dA0JWdL0",
    x = "GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
  )
  def bobKeyX25519_2 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:bob#key-x25519-2"),
    crv = Curve.X25519,
    d = "p-vteoF1gopny1HXywt76xz_uC83UUmrgszsI-ThBKk",
    x = "UT9S3F5ep16KSNBBShU2wh3qSfqYjlasZimn0mB8_VM"
  )
  def bobKeyX25519_3 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:bob#key-x25519-3"),
    crv = Curve.X25519,
    d = "f9WJeuQXEItkGM8shN4dqFr5fLQLBasHnWZ-8dPaSo0",
    x = "82k2BTUiywKv49fKLZa-WwDi8RBf0tB0M8bvSAUQ3yY"
  )
  def bobKeyX25519_NotSecrets1 = OKPPublicKey(
    kty = KTY.OKP,
    kid = Some("did:example:bob#key-x25519-not-secrets-1"),
    crv = Curve.X25519,
    x = "82k2BTUiywKv49fKLZa-WwDi8RBf0tB0M8bvSAUQ3yY"
  )

  def bobKeyP256_1 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p256-1"),
    crv = Curve.`P-256`,
    d = "PgwHnlXxt8pwR6OCTUwwWx-P51BiLkFZyqHzquKddXQ",
    x = "FQVaTOksf-XsCUrt4J1L2UGvtWaDwpboVlqbKBY2AIo",
    y = "6XFB9PYo7dyC5ViJSO9uXNYkxTJWn0d_mqJ__ZYhcNY"
  )
  def bobKeyP256_2 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p256-2"),
    crv = Curve.`P-256`,
    d = "agKz7HS8mIwqO40Q2dwm_Zi70IdYFtonN5sZecQoxYU",
    x = "n0yBsGrwGZup9ywKhzD4KoORGicilzIUyfcXb1CSwe0",
    y = "ov0buZJ8GHzV128jmCw1CaFbajZoFFmiJDbMrceCXIw"
  )
  def bobKeyP256_NotSecrets1 = ECPublicKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p256-not-secrets-1"),
    crv = Curve.`P-256`,
    x = "n0yBsGrwGZup9ywKhzD4KoORGicilzIUyfcXb1CSwe0",
    y = "ov0buZJ8GHzV128jmCw1CaFbajZoFFmiJDbMrceCXIw"
  )

  def bobKeyP384_1 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p384-1"),
    crv = Curve.`P-384`,
    d = "ajqcWbYA0UDBKfAhkSkeiVjMMt8l-5rcknvEv9t_Os6M8s-HisdywvNCX4CGd_xY",
    x = "MvnE_OwKoTcJVfHyTX-DLSRhhNwlu5LNoQ5UWD9Jmgtdxp_kpjsMuTTBnxg5RF_Y",
    y = "X_3HJBcKFQEG35PZbEOBn8u9_z8V1F9V1Kv-Vh0aSzmH-y9aOuDJUE3D4Hvmi5l7"
  )
  def bobKeyP384_2 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p384-2"),
    crv = Curve.`P-384`,
    d = "OiwhRotK188BtbQy0XBO8PljSKYI6CCD-nE_ZUzK7o81tk3imDOuQ-jrSWaIkI-T",
    x = "2x3HOTvR8e-Tu6U4UqMd1wUWsNXMD0RgIunZTMcZsS-zWOwDgsrhYVHmv3k_DjV3",
    y = "W9LLaBjlWYcXUxOf6ECSfcXKaC3-K9z4hCoP0PS87Q_4ExMgIwxVCXUEB6nf0GDd"
  )
  def bobKeyP384_NotSecrets1 = ECPublicKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p384-not-secrets-1"),
    crv = Curve.`P-384`,
    x = "2x3HOTvR8e-Tu6U4UqMd1wUWsNXMD0RgIunZTMcZsS-zWOwDgsrhYVHmv3k_DjV3",
    y = "W9LLaBjlWYcXUxOf6ECSfcXKaC3-K9z4hCoP0PS87Q_4ExMgIwxVCXUEB6nf0GDd"
  )

  def bobKeyP521_1 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p521-1"),
    crv = Curve.`P-521`,
    d = "AV5ocjvy7PkPgNrSuvCxtG70NMj6iTabvvjSLbsdd8OdI9HlXYlFR7RdBbgLUTruvaIRhjEAE9gNTH6rWUIdfuj6",
    x = "Af9O5THFENlqQbh2Ehipt1Yf4gAd9RCa3QzPktfcgUIFADMc4kAaYVViTaDOuvVS2vMS1KZe0D5kXedSXPQ3QbHi",
    y = "ATZVigRQ7UdGsQ9j-omyff6JIeeUv3CBWYsZ0l6x3C_SYqhqVV7dEG-TafCCNiIxs8qeUiXQ8cHWVclqkH4Lo1qH"
  )
  def bobKeyP521_2 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p521-2"),
    crv = Curve.`P-521`,
    d = "ABixMEZHsyT7SRw-lY5HxdNOofTZLlwBHwPEJ3spEMC2sWN1RZQylZuvoyOBGJnPxg4-H_iVhNWf_OtgYODrYhCk",
    x = "ATp_WxCfIK_SriBoStmA0QrJc2pUR1djpen0VdpmogtnKxJbitiPq-HJXYXDKriXfVnkrl2i952MsIOMfD2j0Ots",
    y = "AEJipR0Dc-aBZYDqN51SKHYSWs9hM58SmRY1MxgXANgZrPaq1EeGMGOjkbLMEJtBThdjXhkS5VlXMkF0cYhZELiH"
  )
  def bobKeyP521_NotSecrets1 = ECPublicKey(
    kty = KTY.EC,
    kid = Some("did:example:bob#key-p521-not-secrets-1"),
    crv = Curve.`P-521`,
    x = "ATp_WxCfIK_SriBoStmA0QrJc2pUR1djpen0VdpmogtnKxJbitiPq-HJXYXDKriXfVnkrl2i952MsIOMfD2j0Ots",
    y = "AEJipR0Dc-aBZYDqN51SKHYSWs9hM58SmRY1MxgXANgZrPaq1EeGMGOjkbLMEJtBThdjXhkS5VlXMkF0cYhZELiH"
  )

  // #######################################################

  val bobSecrets = KeyStore(
    Set(
      bobKeyX25519_1,
      bobKeyX25519_2,
      bobKeyX25519_3,
      // bobKeyX25519_NotSecrets1,
      bobKeyP256_1,
      bobKeyP256_2,
      // bobKeyP256_NotSecrets1,
      bobKeyP384_1,
      bobKeyP384_2,
      // bobKeyP384_NotSecrets1,
      bobKeyP521_1,
      bobKeyP521_2,
      // bobKeyP521_NotSecrets1,
    )
  )

  val bobDIDDocument =
    DIDDocumentClass(
      // "@context":["https://www.w3.org/ns/did/v2"],
      id = DIDSubject("did:example:bob"),
      keyAgreement = Some(
        Set(
          bobKeyX25519_1,
          bobKeyX25519_2,
          bobKeyX25519_3,
          bobKeyX25519_NotSecrets1,
          bobKeyP256_1,
          bobKeyP256_2,
          bobKeyP256_NotSecrets1,
          bobKeyP384_1,
          bobKeyP384_2,
          bobKeyP384_NotSecrets1,
          bobKeyP521_1,
          bobKeyP521_2,
          bobKeyP521_NotSecrets1,
        ).map(e => VerificationMethodReferenced(e.kid.get))
      ),
      verificationMethod = Some(
        Set(
          bobKeyX25519_1,
          bobKeyX25519_2,
          bobKeyX25519_3,
          bobKeyX25519_NotSecrets1,
          bobKeyP256_1,
          bobKeyP256_2,
          bobKeyP256_NotSecrets1,
          bobKeyP384_1,
          bobKeyP384_2,
          bobKeyP384_NotSecrets1,
          bobKeyP521_1,
          bobKeyP521_2,
          bobKeyP521_NotSecrets1,
        ).map(e =>
          VerificationMethodEmbeddedJWK(
            id = e.kid.get,
            controller = e.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = e match
              case k: OKPPublicKey  => k.copy(kid = None)
              case k: ECPublicKey   => k.copy(kid = None)
              case k: OKPPrivateKey => k.toPublicKey.copy(kid = None)
              case k: ECPrivateKey  => k.toPublicKey.copy(kid = None)
          )
        )
      ),
      service = Some(
        Set(
          DIDServiceGeneric(
            id = "did:example:bob#didcomm-1",
            `type` = "DIDCommMessaging",
            serviceEndpoint = Json.Arr(
              Json.Obj(
                ("uri", Json.Str("http://example.com/path")),
                ("accept", Json.Arr(Json.Str("didcomm/v2"), Json.Str("didcomm/aip2;env=rfc587"))),
                ("routing_keys", Json.Arr(Json.Str("did:example:mediator1#key-x25519-1"))),
              )
            )
          )
        )
      )
    )

}
