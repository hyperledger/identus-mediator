package fmgp.did

import zio.json._
import fmgp.crypto._
import fmgp.did._

object DidExampleSicpaRustMediator1 {

  // #################################
  // ### did:example:mediator1 Key ###
  // #################################

  def mediator1KeyX25519 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:mediator1#key-x25519-1"),
    crv = Curve.X25519,
    d = "b9NnuOCB0hm7YGNvaE9DMhwH_wjZA1-gWD6dA0JWdL0",
    x = "GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
  )
  def mediator1KeyP256 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:mediator1#key-p256-1"),
    crv = Curve.`P-256`,
    d = "PgwHnlXxt8pwR6OCTUwwWx-P51BiLkFZyqHzquKddXQ",
    x = "FQVaTOksf-XsCUrt4J1L2UGvtWaDwpboVlqbKBY2AIo",
    y = "6XFB9PYo7dyC5ViJSO9uXNYkxTJWn0d_mqJ__ZYhcNY"
  )
  def mediator1KeyP384 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:mediator1#key-p384-1"),
    crv = Curve.`P-384`,
    d = "ajqcWbYA0UDBKfAhkSkeiVjMMt8l-5rcknvEv9t_Os6M8s-HisdywvNCX4CGd_xY",
    x = "MvnE_OwKoTcJVfHyTX-DLSRhhNwlu5LNoQ5UWD9Jmgtdxp_kpjsMuTTBnxg5RF_Y",
    y = "X_3HJBcKFQEG35PZbEOBn8u9_z8V1F9V1Kv-Vh0aSzmH-y9aOuDJUE3D4Hvmi5l7"
  )
  def mediator1KeyP521 = ECPrivateKey(
    kty = KTY.EC,
    kid = Some("did:example:mediator1#key-p521-1"),
    crv = Curve.`P-521`,
    d = "AV5ocjvy7PkPgNrSuvCxtG70NMj6iTabvvjSLbsdd8OdI9HlXYlFR7RdBbgLUTruvaIRhjEAE9gNTH6rWUIdfuj6",
    x = "Af9O5THFENlqQbh2Ehipt1Yf4gAd9RCa3QzPktfcgUIFADMc4kAaYVViTaDOuvVS2vMS1KZe0D5kXedSXPQ3QbHi",
    y = "ATZVigRQ7UdGsQ9j-omyff6JIeeUv3CBWYsZ0l6x3C_SYqhqVV7dEG-TafCCNiIxs8qeUiXQ8cHWVclqkH4Lo1qH"
  )

  // // #######################################################

  val mediator1Secrets = KeyStore(
    Set(
      mediator1KeyX25519,
      mediator1KeyP256,
      mediator1KeyP384,
      mediator1KeyP521,
    )
  )

  val mediator1DIDDocument =
    DIDDocumentClass(
      // "@context":["https://www.w3.org/ns/did/v2"],
      id = DIDSubject("did:example:mediator1"),
      keyAgreement = Some(
        Set(
          VerificationMethodReferenced("did:example:mediator1#key-x25519-1"),
          VerificationMethodReferenced("did:example:mediator1#key-p256-1"),
          VerificationMethodReferenced("did:example:mediator1#key-p384-1"),
          VerificationMethodReferenced("did:example:mediator1#key-p521-1"),
        )
      ),
      verificationMethod = Some(
        Set(
          VerificationMethodEmbeddedJWK(
            id = mediator1KeyX25519.kid.get,
            controller = mediator1KeyX25519.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = mediator1KeyX25519.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = mediator1KeyP256.kid.get,
            controller = mediator1KeyP256.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = mediator1KeyP256.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = mediator1KeyP384.kid.get,
            controller = mediator1KeyP384.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = mediator1KeyP384.toPublicKey.copy(kid = None)
          ),
          VerificationMethodEmbeddedJWK(
            id = mediator1KeyP521.kid.get,
            controller = mediator1KeyP521.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = mediator1KeyP521.toPublicKey.copy(kid = None)
          ),
        )
      )
    )

}
