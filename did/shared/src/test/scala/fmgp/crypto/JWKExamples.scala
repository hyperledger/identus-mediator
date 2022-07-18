package fmgp.crypto

object JWKExamples {

  // ##################
  // ### Sender Key ###
  // ##################

  /** key-1 */
  val senderKeyEd25519 = """{
    "kid":"did:example:alice#key-1",
    "kty":"OKP",
    "d":"pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
    "crv":"Ed25519",
    "x":"G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww"
  }""".stripMargin

  /** key-2 */
  val senderKeyP256 = """{
    "kid":"did:example:alice#key-2",
    "kty":"EC",
    "d":"7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A",
    "crv":"P-256",
    "x":"2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY",
    "y":"BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w"
  }""".stripMargin

  /** key-3 */
  val senderKeySecp256k1 = """{
    "kid":"did:example:alice#key-3",
    "kty":"EC",
    "d":"N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA",
    "crv":"secp256k1",
    "x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
    "y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
  }""".stripMargin

  val senderKeySecp256k1obj = ECPrivateKey(
    kty = KTY.EC,
    crv = Curve.secp256k1,
    kid = Some("did:example:alice#key-3"),
    d = "N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA",
    x = "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
    y = "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
  )

  /** key-4 */
  val senderKeyX25519 = """{
    "kid":"did:example:alice#key-x25519-1",
    "kty":"OKP",
    "d":"r-jK2cO3taR8LQnJB1_ikLBTAnOtShJOsHXRUWT-aZA",
    "crv":"X25519",
    "x":"avH0O2Y4tqLAq8y9zpianr8ajii5m4F_mICrzNlatXs"
  }""".stripMargin

  /** key-5 */
  val senderKeyP256_2 = """{
    "kid":"did:example:alice#key-p256-1",
    "kty":"EC",
    "d":"sB0bYtpaXyp-h17dDpMx91N3Du1AdN4z1FUq02GbmLw",
    "crv":"P-256",
    "x":"L0crjMN1g0Ih4sYAJ_nGoHUck2cloltUpUVQDhF2nHE",
    "y":"SxYgE7CmEJYi7IDhgK5jI4ZiajO8jPRZDldVhqFpYoo"
  }""".stripMargin

  /** key-6 */
  val senderKeyP521 = """{
    "kid":"did:example:alice#key-p521-1",
    "kty":"EC",
    "d":"AQCQKE7rZpxPnX9RgjXxeywrAMp1fJsyFe4cir1gWj-8t8xWaM_E2qBkTTzyjbRBu-JPXHe_auT850iYmE34SkWi",
    "crv":"P-521",
    "x":"AHBEVPRhAv-WHDEvxVM9S0px9WxxwHL641Pemgk9sDdxvli9VpKCBdra5gg_4kupBDhz__AlaBgKOC_15J2Byptz",
    "y":"AciGcHJCD_yMikQvlmqpkBbVqqbg93mMVcgvXBYAQPP-u9AF7adybwZrNfHWCKAQwGF9ugd0Zhg7mLMEszIONFRk"
  }""".stripMargin

  // #####################
  // ### Recipient Key ###
  // #####################

  val recipientKey1 = """{
    "kid":"did:example:bob#key-x25519-1",
    "kty":"OKP",
    "d":"b9NnuOCB0hm7YGNvaE9DMhwH_wjZA1-gWD6dA0JWdL0",
    "crv":"X25519",
    "x":"GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
  }""".stripMargin
  val recipientKey2 = """{
    "kid":"did:example:bob#key-x25519-2",
    "kty":"OKP",
    "d":"p-vteoF1gopny1HXywt76xz_uC83UUmrgszsI-ThBKk",
    "crv":"X25519",
    "x":"UT9S3F5ep16KSNBBShU2wh3qSfqYjlasZimn0mB8_VM"
  }""".stripMargin
  val recipientKey3 = """{
    "kid":"did:example:bob#key-x25519-3",
    "kty":"OKP",
    "d":"f9WJeuQXEItkGM8shN4dqFr5fLQLBasHnWZ-8dPaSo0",
    "crv":"X25519",
    "x":"82k2BTUiywKv49fKLZa-WwDi8RBf0tB0M8bvSAUQ3yY"
  }""".stripMargin
  val recipientKey4 = """{
    "kid":"did:example:bob#key-p256-1",
    "kty":"EC",
    "d":"PgwHnlXxt8pwR6OCTUwwWx-P51BiLkFZyqHzquKddXQ",
    "crv":"P-256",
    "x":"FQVaTOksf-XsCUrt4J1L2UGvtWaDwpboVlqbKBY2AIo",
    "y":"6XFB9PYo7dyC5ViJSO9uXNYkxTJWn0d_mqJ__ZYhcNY"
  }""".stripMargin
  val recipientKey5 = """{
    "kid":"did:example:bob#key-p256-2",
    "kty":"EC",
    "d":"agKz7HS8mIwqO40Q2dwm_Zi70IdYFtonN5sZecQoxYU",
    "crv":"P-256",
    "x":"n0yBsGrwGZup9ywKhzD4KoORGicilzIUyfcXb1CSwe0",
    "y":"ov0buZJ8GHzV128jmCw1CaFbajZoFFmiJDbMrceCXIw"
  }""".stripMargin
  val recipientKey6 = """{
    "kid":"did:example:bob#key-p384-1",
    "kty":"EC",
    "d":"ajqcWbYA0UDBKfAhkSkeiVjMMt8l-5rcknvEv9t_Os6M8s-HisdywvNCX4CGd_xY",
    "crv":"P-384",
    "x":"MvnE_OwKoTcJVfHyTX-DLSRhhNwlu5LNoQ5UWD9Jmgtdxp_kpjsMuTTBnxg5RF_Y",
    "y":"X_3HJBcKFQEG35PZbEOBn8u9_z8V1F9V1Kv-Vh0aSzmH-y9aOuDJUE3D4Hvmi5l7"
  }""".stripMargin
  val recipientKey7 = """{
    "kid":"did:example:bob#key-p384-2",
    "kty":"EC",
    "d":"OiwhRotK188BtbQy0XBO8PljSKYI6CCD-nE_ZUzK7o81tk3imDOuQ-jrSWaIkI-T",
    "crv":"P-384",
    "x":"2x3HOTvR8e-Tu6U4UqMd1wUWsNXMD0RgIunZTMcZsS-zWOwDgsrhYVHmv3k_DjV3",
    "y":"W9LLaBjlWYcXUxOf6ECSfcXKaC3-K9z4hCoP0PS87Q_4ExMgIwxVCXUEB6nf0GDd"
  }""".stripMargin
  val recipientKey8 = """{
    "kid":"did:example:bob#key-p521-1",
    "kty":"EC",
    "d":"AV5ocjvy7PkPgNrSuvCxtG70NMj6iTabvvjSLbsdd8OdI9HlXYlFR7RdBbgLUTruvaIRhjEAE9gNTH6rWUIdfuj6",
    "crv":"P-521",
    "x":"Af9O5THFENlqQbh2Ehipt1Yf4gAd9RCa3QzPktfcgUIFADMc4kAaYVViTaDOuvVS2vMS1KZe0D5kXedSXPQ3QbHi",
    "y":"ATZVigRQ7UdGsQ9j-omyff6JIeeUv3CBWYsZ0l6x3C_SYqhqVV7dEG-TafCCNiIxs8qeUiXQ8cHWVclqkH4Lo1qH"
  }""".stripMargin
  val recipientKey9 = """{
    "kid":"did:example:bob#key-p521-2",
    "kty":"EC",
    "d":"ABixMEZHsyT7SRw-lY5HxdNOofTZLlwBHwPEJ3spEMC2sWN1RZQylZuvoyOBGJnPxg4-H_iVhNWf_OtgYODrYhCk",
    "crv":"P-521",
    "x":"ATp_WxCfIK_SriBoStmA0QrJc2pUR1djpen0VdpmogtnKxJbitiPq-HJXYXDKriXfVnkrl2i952MsIOMfD2j0Ots",
    "y":"AEJipR0Dc-aBZYDqN51SKHYSWs9hM58SmRY1MxgXANgZrPaq1EeGMGOjkbLMEJtBThdjXhkS5VlXMkF0cYhZELiH"
  }""".stripMargin

}
