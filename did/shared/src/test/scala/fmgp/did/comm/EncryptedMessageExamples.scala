package fmgp.did.comm

object EncryptedMessageExamples {

  def plaintextMessage = """{
   "id":"1234567890",
   "type":"https://example.com/protocols/lets_do_lunch/1.0/proposal",
   "from":"did:example:alice",
   "to":[
      "did:example:bob"
   ],
   "created_time":1516269022,
   "expires_time":1516385931,
   "body":{
      "messagespecificattribute":"and its value"
   }
}"""

  def allEncryptedMessage = Seq(
    encryptedMessage_ECDHES_X25519_XC20P,
    encryptedMessage_ECDHES_P384_A256CBCHS512,
    encryptedMessage_ECDHES_P521_A256GCM,
    encryptedMessage_ECDH1PU_X25519_A256CBCHS512,
    encryptedMessage_EdDSA_ECDH1PU_P521_A256CBCHS512,
    encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P,
  )

  /** This example uses ECDH-ES key wrapping algorithm using key with X25519 elliptic curve and XC20P for content
    * encryption of the message.
    *
    * {"epk":{"kty":"OKP","crv":"X25519","x":"JHjsmIRZAaB0zRG_wNXLV2rPggF00hdHbW5rj8g0I24"},
    * "apv":"NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA", "typ":"application/didcomm-encrypted+json", "enc":"XC20P",
    * "alg":"ECDH-ES+A256KW"}
    */
  def encryptedMessage_ECDHES_X25519_XC20P = """{
   "ciphertext":"KWS7gJU7TbyJlcT9dPkCw-ohNigGaHSukR9MUqFM0THbCTCNkY-g5tahBFyszlKIKXs7qOtqzYyWbPou2q77XlAeYs93IhF6NvaIjyNqYklvj-OtJt9W2Pj5CLOMdsR0C30wchGoXd6wEQZY4ttbzpxYznqPmJ0b9KW6ZP-l4_DSRYe9B-1oSWMNmqMPwluKbtguC-riy356Xbu2C9ShfWmpmjz1HyJWQhZfczuwkWWlE63g26FMskIZZd_jGpEhPFHKUXCFwbuiw_Iy3R0BIzmXXdK_w7PZMMPbaxssl2UeJmLQgCAP8j8TukxV96EKa6rGgULvlo7qibjJqsS5j03bnbxkuxwbfyu3OxwgVzFWlyHbUH6p",
   "protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkpIanNtSVJaQWFCMHpSR193TlhMVjJyUGdnRjAwaGRIYlc1cmo4ZzBJMjQifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0",
   "recipients":[
      {
         "encrypted_key":"3n1olyBR3nY7ZGAprOx-b7wYAKza6cvOYjNwVg3miTnbLwPP_FmE1A",
         "header":{
            "kid":"did:example:bob#key-x25519-1"
         }
      },
      {
         "encrypted_key":"j5eSzn3kCrIkhQAWPnEwrFPMW6hG0zF_y37gUvvc5gvlzsuNX4hXrQ",
         "header":{
            "kid":"did:example:bob#key-x25519-2"
         }
      },
      {
         "encrypted_key":"TEWlqlq-ao7Lbynf0oZYhxs7ZB39SUWBCK4qjqQqfeItfwmNyDm73A",
         "header":{
            "kid":"did:example:bob#key-x25519-3"
         }
      }
   ],
   "tag":"6ylC_iAs4JvDQzXeY6MuYQ",
   "iv":"ESpmcyGiZpRjc5urDela21TOOTW8Wqd1"
}""".stripMargin

  /** This example uses ECDH-ES key wrapping algorithm using key with NIST defined P-384 elliptic curve and
    * A256CBC-HS512 for content encryption of the message.
    *
    * {"epk":{"kty":"EC","crv":"P-384","x":"161agGeahGemHgnjHmQ_B_SONyBVg8VLdhTgV5W54VbabyllissnZ6W779IoUqKr",
    * "y":"cCeqeFgobo_cR-Y4TsZBZX8u3BkiyNs2b-vdqOqOLyCnVgO2jo7nsA_IC3ant9OX"},
    * "apv":"LJA9Eoks5tamUFVBalMwBhJ6DkDcJ8HK4SlXZWqDqno", "typ":"application/didcomm-encrypted+json",
    * "enc":"A256CBC-HS512", "alg":"ECDH-ES+A256KW" }
    */
  def encryptedMessage_ECDHES_P384_A256CBCHS512 = """{
   "ciphertext":"HPnc9w7jK0T73Spifq_dcVJnONbT9MZ9oorDJFEBJAfmwYRqvs1rKue-udrNLTTH0qjjbeuji01xPRF5JiWyy-gSMX4LHdLhPxHxjjQCTkThY0kapofU85EjLPlI4ytbHiGcrPIezqCun4iDkmb50pwiLvL7XY1Ht6zPUUdhiV6qWoPP4qeY_8pfH74Q5u7K4TQ0uU3KP8CVZQuafrkOBbqbqpJV-lWpWIKxil44f1IT_GeIpkWvmkYxTa1MxpYBgOYa5_AUxYBumcIFP-b6g7GQUbN-1SOoP76EzxZU_louspzQ2HdEH1TzXw2LKclN8GdxD7kB0H6lZbZLT3ScDzSVSbvO1w1fXHXOeOzywuAcismmoEXQGbWZm7wJJJ2r",
   "protected":"eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTM4NCIsIngiOiIxNjFhZ0dlYWhHZW1IZ25qSG1RX0JfU09OeUJWZzhWTGRoVGdWNVc1NFZiYWJ5bGxpc3NuWjZXNzc5SW9VcUtyIiwieSI6ImNDZXFlRmdvYm9fY1ItWTRUc1pCWlg4dTNCa2l5TnMyYi12ZHFPcU9MeUNuVmdPMmpvN25zQV9JQzNhbnQ5T1gifSwiYXB2IjoiTEpBOUVva3M1dGFtVUZWQmFsTXdCaEo2RGtEY0o4SEs0U2xYWldxRHFubyIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC1FUytBMjU2S1cifQ",
   "recipients":[
      {
         "encrypted_key":"SlyWCiOaHMMH9CqSs2CHpRd2XwbueZ1-MfYgKVepXWpgmTgtsgNOAaYwV5pxK3D67HV51F-vLBFlAHke7RYp_GeGDFYhAf5s",
         "header":{
            "kid":"did:example:bob#key-p384-1"
         }
      },
      {
         "encrypted_key":"5e7ChtaRgIlV4yS4NSD7kEo0iJfFmL_BFgRh3clDKBG_QoPd1eOtFlTxFJh-spE0khoaw8vEEYTcQIg4ReeFT3uQ8aayz1oY",
         "header":{
            "kid":"did:example:bob#key-p384-2"
         }
      }
   ],
   "tag":"bkodXkuuwRbqksnQNsCM2YLy9f0v0xNgnhSUAoFGtmE",
   "iv":"aE1XaH767m7LY0JTN7RsAA"
}""".stripMargin

  /** This example uses ECDH-ES key wrapping algorithm using key with NIST defined P-521 elliptic curve and A256GCM for
    * content encryption of the message.
    *
    * {"epk":{ "kty":"EC","crv":"P-521",
    * "x":"AEkksOZmmhfFXuOt0s2mWEbUrmT79w5HTpRoS-6Y5zdbY9OB9odGohCbmOxjjceaYE9fsZ_tZ6giLaA5ADRpkXNU",
    * "y":"ACibg-vD2aGTJGo9fEIzCWWOhRUINlX7CXFI2jx9JT6fO2_0fwK36Y7-4sTe4iEURhygSXP9oSTW3NGYMuC1iOCp" },
    * "apv":"GOeo76ym6NCg9WWMEYfW0eVDT5668zEhl2uAIW-E-HE", "typ":"application/didcomm-encrypted+json",
    * "enc":"A256GCM","alg":"ECDH-ES+A256KW" }
    */
  def encryptedMessage_ECDHES_P521_A256GCM = """{
   "ciphertext":"mxnFl4s8FRsIJIBVcRLv4gj4ru5R0H3BdvyBWwXV3ILhtl_moqzx9COINGomP4ueuApuY5xdMDvRHm2mLo6N-763wjNSjAibNrqVZC-EG24jjYk7RPZ26fEW4z87LHuLTicYCD4yHqilRbRgbOCT0Db5221Kec0HDZTXLzBqVwC2UMyDF4QT6Uz3fE4f_6BXTwjD-sEgM67wWTiWbDJ3Q6WyaOL3W4ukYANDuAR05-SXVehnd3WR0FOg1hVcNRao5ekyWZw4Z2ekEB1JRof3Lh6uq46K0KXpe9Pc64UzAxEID93SoJ0EaV_Sei8CXw2aJFmZUuCf8YISWKUz6QZxRvFKUfYeflldUm9U2tY96RicWgUhuXgv",
   "protected":"eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTUyMSIsIngiOiJBRWtrc09abW1oZkZYdU90MHMybVdFYlVybVQ3OXc1SFRwUm9TLTZZNXpkYlk5T0I5b2RHb2hDYm1PeGpqY2VhWUU5ZnNaX3RaNmdpTGFBNUFEUnBrWE5VIiwieSI6IkFDaWJnLXZEMmFHVEpHbzlmRUl6Q1dXT2hSVUlObFg3Q1hGSTJqeDlKVDZmTzJfMGZ3SzM2WTctNHNUZTRpRVVSaHlnU1hQOW9TVFczTkdZTXVDMWlPQ3AifSwiYXB2IjoiR09lbzc2eW02TkNnOVdXTUVZZlcwZVZEVDU2Njh6RWhsMnVBSVctRS1IRSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiRUNESC1FUytBMjU2S1cifQ",
   "recipients":[
      {
         "encrypted_key":"W4KOy5W88iPPsDEdhkJN2krZ2QAeDxOIxW-4B21H9q89SHWexocCrw",
         "header":{
            "kid":"did:example:bob#key-p521-1"
         }
      },
      {
         "encrypted_key":"uxKPkF6-sIiEkdeJcUPJY4lvsRg_bvtLPIn7eIycxLJML2KM6-Llag",
         "header":{
            "kid":"did:example:bob#key-p521-2"
         }
      }
   ],
   "tag":"aPZeYfwht2Nx9mfURv3j3g",
   "iv":"lGKCvg2xrvi8Qa_D"
}""".stripMargin

  /** This example uses ECDH-1PU key wrapping algorithm using key with X25519 elliptic curve and A256CBC-HS512 for
    * content encryption of the message.
    *
    * {"epk":{"kty":"OKP","crv":"X25519","x":"GFcMopJljf4pLZfch4a_GhTM_YAf6iNI1dWDGyVCaw0"},
    * "apv":"NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA", "skid":"did:example:alice#key-x25519-1",
    * "apu":"ZGlkOmV4YW1wbGU6YWxpY2Uja2V5LXgyNTUxOS0x", "typ":"application/didcomm-encrypted+json",
    * "enc":"A256CBC-HS512","alg":"ECDH-1PU+A256KW"}
    */
  def encryptedMessage_ECDH1PU_X25519_A256CBCHS512 = """{
   "ciphertext":"MJezmxJ8DzUB01rMjiW6JViSaUhsZBhMvYtezkhmwts1qXWtDB63i4-FHZP6cJSyCI7eU-gqH8lBXO_UVuviWIqnIUrTRLaumanZ4q1dNKAnxNL-dHmb3coOqSvy3ZZn6W17lsVudjw7hUUpMbeMbQ5W8GokK9ZCGaaWnqAzd1ZcuGXDuemWeA8BerQsfQw_IQm-aUKancldedHSGrOjVWgozVL97MH966j3i9CJc3k9jS9xDuE0owoWVZa7SxTmhl1PDetmzLnYIIIt-peJtNYGdpd-FcYxIFycQNRUoFEr77h4GBTLbC-vqbQHJC1vW4O2LEKhnhOAVlGyDYkNbA4DSL-LMwKxenQXRARsKSIMn7z-ZIqTE-VCNj9vbtgR",
   "protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkdGY01vcEpsamY0cExaZmNoNGFfR2hUTV9ZQWY2aU5JMWRXREd5VkNhdzAifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInNraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXkteDI1NTE5LTEiLCJhcHUiOiJaR2xrT21WNFlXMXdiR1U2WVd4cFkyVWphMlY1TFhneU5UVXhPUzB4IiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ",
   "recipients":[
      {
         "encrypted_key":"o0FJASHkQKhnFo_rTMHTI9qTm_m2mkJp-wv96mKyT5TP7QjBDuiQ0AMKaPI_RLLB7jpyE-Q80Mwos7CvwbMJDhIEBnk2qHVB",
         "header":{
            "kid":"did:example:bob#key-x25519-1"
         }
      },
      {
         "encrypted_key":"rYlafW0XkNd8kaXCqVbtGJ9GhwBC3lZ9AihHK4B6J6V2kT7vjbSYuIpr1IlAjvxYQOw08yqEJNIwrPpB0ouDzKqk98FVN7rK",
         "header":{
            "kid":"did:example:bob#key-x25519-2"
         }
      },
      {
         "encrypted_key":"aqfxMY2sV-njsVo-_9Ke9QbOf6hxhGrUVh_m-h_Aq530w3e_4IokChfKWG1tVJvXYv_AffY7vxj0k5aIfKZUxiNmBwC_QsNo",
         "header":{
            "kid":"did:example:bob#key-x25519-3"
         }
      }
   ],
   "tag":"uYeo7IsZjN7AnvBjUZE5lNryNENbf6_zew_VC-d4b3U",
   "iv":"o02OXDQ6_-sKz2PX_6oyJg"
}""".stripMargin

  /** In this example, the message is first signed with EdDSA digital signature and then encrypted with ECDH-1PU key
    * wrapping algorithm using key with NIST defined P-521 elliptic curve and A256CBC-HS512 for content encryption of
    * the message.
    *
    * {"epk":{"kty":"EC","crv":"P-256","x":"NlrwPvtIIneciyEka4s2-4s8OjTbtFEAXfL-vglyzqo","y":"hb2vdXNsK5BCe7-XZCG_-64GmTO_k5IMXPZCM1taTBg"},
    * "apv":"z-LqpvVXDb_sGYn3mjQLpuu2CQLewYuZoTWOIXPH3FM", "skid":"did:example:alice#key-p256-1",
    * "apu":"ZGlkOmV4YW1wbGU6YWxpY2Uja2V5LXAyNTYtMQ", "typ":"application/didcomm-encrypted+json", "enc":"A256CBC-HS512",
    * "alg":"ECDH-1PU+A256KW"}
    */
  def encryptedMessage_EdDSA_ECDH1PU_P521_A256CBCHS512 = """{
   "ciphertext":"WCufCs2lMZfkxQ0JCK92lPtLFgwWk_FtRWOMj52bQISa94nEbIYqHDUohIbvLMgbSjRcJVusZO04UthDuOpSSTcV5GBi3O0cMrjyI_PZnTb1yikLXpXma1bT10D2r5TPtzRMxXF3nFsr9y0JKV1TsMtn70Df2fERx2bAGxcflmd-A2sMlSTT8b7QqPtn17Yb-pA8gr4i0Bqb2WfDzwnbfewbukpRmPA2hsEs9oLKypbniAafSpoiQjfb19oDfsYaWWXqsdjTYMflqH__DqSmW52M-SUp6or0xU0ujbHmOkRkcdh9PsR5YsPuIWAqYa2hfjz_KIrGTxvCos0DMiZ4Lh_lPIYQqBufSdFH5AGChoekFbQ1vcyIyYMFugzOHOgZ2TwEzv94GCgokBHQR4_qaU_f4Mva64KPwqOYdm5f4KX16afTJa-IV7ar7__2L-A-LyxmC5KIHeGOedV9kzZBLC7TuzRAuE3vY7pkhLB1jPE6XpTeKXldljaeOSEVcbFUQtsHOSPz9JXuhqZ1fdAx8qV7hUnSAd_YMMDR3S6SXtem8ak2m98WPvKIxhCbcto7W2qoNYMT7MPvvid-QzUvTdKtyovCvLzhyYJzMjZxmn9-EnGhZ5ITPL_xFfLyKxhSSUVz3kSwK9xuOj3KpJnrrD7xrp5FKzEaJVIHWrUW90V_9QVLjriThZ36fA3ipvs8ZJ8QSTnGAmuIQ6Z2u_r4KsjL_mGAgn47qyqRm-OSLEUE4_2qB0Q9Z7EBKakCH8VPt09hTMDR62aYZYwtmpNs9ISu0VPvFjh8UmKbFcQsVrz90-x-r-Q1fTX9JaIFcDy7aqKcI-ai3tVF_HDR60Jaiw",
   "protected":"eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTI1NiIsIngiOiJObHJ3UHZ0SUluZWNpeUVrYTRzMi00czhPalRidEZFQVhmTC12Z2x5enFvIiwieSI6ImhiMnZkWE5zSzVCQ2U3LVhaQ0dfLTY0R21UT19rNUlNWFBaQ00xdGFUQmcifSwiYXB2Ijoiei1McXB2VlhEYl9zR1luM21qUUxwdXUyQ1FMZXdZdVpvVFdPSVhQSDNGTSIsInNraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktcDI1Ni0xIiwiYXB1IjoiWkdsa09tVjRZVzF3YkdVNllXeHBZMlVqYTJWNUxYQXlOVFl0TVEiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLWVuY3J5cHRlZCtqc29uIiwiZW5jIjoiQTI1NkNCQy1IUzUxMiIsImFsZyI6IkVDREgtMVBVK0EyNTZLVyJ9",
   "recipients":[
      {
         "encrypted_key":"ZIL6Leligq1Xps_229nlo1xB_tGxOEVoEEMF-XTOltI0QXjyUoq_pFQBCAnVdcWNH5bmaiuzCYOmZ9lkyXBkfHO90KkGgODG",
         "header":{
            "kid":"did:example:bob#key-p256-1"
         }
      },
      {
         "encrypted_key":"sOjs0A0typIRSshhQoiJPoM4o7YpR5LA8SSieHZzmMyIDdD8ww-4JyyQhqFYuvfS4Yt37VF4z7Nd0OjYVNRL-iqPnoJ3iCOr",
         "header":{
            "kid":"did:example:bob#key-p256-2"
         }
      }
   ],
   "tag":"nIpa3EQ29hgCkA2cBPde2HpKXK4_bvmL2x7h39rtVEc",
   "iv":"mLqi1bZLz7VwqtVVFsDiLg"
}""".stripMargin

  /** In this example, the message is first signed with EdDSA digital signature and then encrypted with ECDH-1PU key
    * wrapping algorithm using key with X25519 elliptic curve and A256CBC-HS512 for content encryption of the message.
    * After that the message is encrypted a second time with ECDH-ES key wrapping algorithm using key with X25519
    * elliptic curve and XC20P for content encryption of the message. The second anoncrypt is used to protect the sender
    * ID.
    */
  def encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P = """{
   "ciphertext":"lfYmR7CNas5hOePxWQEkUEwzSRds3t5GkMW4VUZKJWJ7H3y1X8a1RnUg3c0BCqdszzhZk8xE0vfQ67vJAWGdev8OWy7oGY_e1o4iAVj3mPNfnV5N7sjld6yUhrxqDsxtmVAp7LAipbJNhxqBoEXdb8hPbdPeUIov-5X0_cQHpHalSD6zMoyUPb0cCnw8bfmdN3aaVDrzsZRIkvhezZCkaQFMO75XKVEDyTzn8Eqwgpg_tzD_Hr00jHa9mTyTiDA_1ZzqleF-XSe5NEtFc7_BukgjPWMZAouPMWwIP0h-BPULxUzYcWKfC6hiU2ZuxWz8Fs8v9r6MCAaPOG37oA_yfWwE_FWl7x61sl6iZfDVQhOTkdlXNoZ0LiaC4ImXop2wSvKimkGqhysj1OefrUrpHmSx1qNz7vCWqW8Mo7fykXQCVYr6zXmcvWF5-KvXDu6DR3EFlgs6An9tWLv1flDrZWb-lS6RlL6Z8AqmLjP0Yb2r6mTopiulTTpXXpwe-Qs1_DHDGi0DfsZmcYhyra-F8YQ3tGIgy6wWCtyBh7Fq_zRy8RMvV3DkaLHYTekIle0YOoRdZRJBb3ycXHycIi7iT1ewOFlIGjsBg73Hkqa6O1weewS3uIxl4veO6cBOksfDRpC279X9tV1HDqROBolNBsWHQ2UpUD1Bat8UnfJMrwBcZkGQCjhlR9SSlZzEIqP3leRh5e2y2FGTm7wNRNwmgl6s6OUiKD-nbUnnSugGzolbavafHS80XrdfEuUyuPjnpQQQROapFfcjd7dSLd58g9OjOEqb1-Edk4KcW-yYU17_zfIzv1qykEH7F22Nq9HGbReXuao83ItUWgpBDZ-uf-_RbcpW2X1U5QGnI1SF4Trbhx74lnswEF_AlZ4SUh7frcMfKQLYobT1X_wIEY8pwN1AzWf482LJKKsxm0EcY73vf0n3uT_OS3EgBNCVYyF6_snm7MdOV-RM5ZZyQl64BsZ4aL4RVVCOa8bxYGPxvpOf9Ay-aQjwYQfyFxayRJiQWkywk8SRAdLLfSiveqvXAoIIi_XI98CRIaJ6DSKr-TuCDlz4yVP_8emS_S0S7F-Buh-P6nzjdJ04CAm95p6do_q8jk1IRHvubqrPKcpvk4U3p-6obJK9feJPffoe3-ddJvKJ5h8Et3xEKG7oId3NkbbFfYUnkEyC_wUeKtyrXK8uBz5HKhW1S27qsBAnKv5WTCyfrDsfX0eTaqdeJ3O9uR4niBc2sa2t89G5AEKWcOUnJcytAAAuhMZiz2zXXhmffPG5A7QSmZMAl75CP6ulN0KCBE0nTeuvNPueqpF4PV4CCcMfokz0hu5k5oo9FHfkQMVDBTiQUtEezIXiglqhu6VwcDgbbatAKUIYxnoisHKPg17zGMl5VMULVY5WBYPAUylKpWELnMc9BHUHNUxfSVlqdd847v__D1Go17MTsQujVGQQuM61Ay0-z1JwN0fki0M8t20U_sWX5jNMbdZCPBxy7rpZlztaF01j1NCaM3ZPh-_KLy8vQ584R5I5LlE5OejgyLQYMOMzSgUZZEAeTGV_S-kEnt36k-L8Kbyv_LWuiuTQzwLSwlmWOKLdDbmjEjA1JsEaKmorDKz0q7MFIoC-gKKJBjPTJ5PxJLJj4RHOxxDWhx00HjLLE3S1B6uAvKVUhN4ka_wWusVqffrRZm_e7Oz0hbCO8pT4tzlbFWTu0-O44kHkRjfubEi4PnaNzKbGMXTrDo7aY6sgiDB8KlJSsKrNeG0OLjBAYF_zmHlrqctFQidTD_YIDzcSfkCTrMoOYa07nXG6E1nArScOgkNuNkPVhCq_VD6w-pZ1mSUBwKVCnjNueTrB5RvFBydaoWcAAX3OtH8yFeDWGzlRYWJNKEKull_Vah8B7nwwnTPxyeUwnr2txlwDvLx9ASrl5CjwvLc9bL7jCa6SrWt3hPjvjDY4JdFxnCqyyXD11Mpt2kyA4TTBaBbzI5Kja6pKsCUw0QCTCfTBu7bKGTOJKai32c4WRXvpVgIowOzdyjtKD0LgnY2fRTpJWpcTMVAHPfSad0jc23iTwOKcJQ0n_ExfOxzW_PSvAYbakrRwdZdDefb_fLrILxgS7OA9KepGQOJnp0-X_o1bBkXsm_cvVhcprLViUxHR1uCTMXaUl24viekps45aODvfBj5OsG3GrEShqtLb7ukEHEJjLsIe1l-4kFtNp4RlPZlapYgNyMSjnGopw2D51khuOHdJ2yLWASgFJPIa4dan4KTcDhp7qmbijN8JR_s_p1DB4E1nFlQPuncA8lIiuGv2PKHKXQkkuHcKmPMYTjRlam5IBHXQPV_njHMAIV60XU8kxa5G7t-Iwl_6OeRIj_HXdf5mfdTNEYlwbQWHInkS4U32RD9Kf0u6SC1bpRZx6AbFK8xlIgUPhB_sP3kG_ZZIZhcJ1Oy6Q7pAzmKXZYWKMkDWZk7a-WsiA0Z8gOcd7PYA13GRIw0MT_GIRcFRfkp7821j2ArHHo6jagqMdEuCZHzHrfwD0XHzT4FP3-aTaHIqrKx0TiYRfn2k2Q",
   "protected":"eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTUyMSIsIngiOiJBYmxoeVVENUxYNE9zWDhGRTVaODRBX09CYThiOHdhVUhXSFExbTBnczhuSFVERDdySDlJRWRZbzJUSzFQYU5ha05aSk54a1FBWC1aUkxWa1BoNnV4eTJNIiwieSI6IkFQTjh6c0xEZGJpVjN0LTloWTJFQzFVZWEzTm5tMzFtNWowRmNiUWM0Y2ZWQmFNdzVCQ2VpcU9QWkljZTVMNjI4bnVORkxKR2szSjh6SVBPYUlLU0xmaTEifSwiYXB2IjoiR09lbzc2eW02TkNnOVdXTUVZZlcwZVZEVDU2Njh6RWhsMnVBSVctRS1IRSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0",
   "recipients":[
      {
         "encrypted_key":"iuVx5qAiRtijMfHnkF95_ByjHyiAmRqNTrExrEQK4p7HwW7sit1F0g",
         "header":{
            "kid":"did:example:bob#key-p521-1"
         }
      },
      {
         "encrypted_key":"6OWnv-tY1ZDUBt8uRNpmteoXTVDzRGz2UF04Y2eh2-bp2jiViU8VCw",
         "header":{
            "kid":"did:example:bob#key-p521-2"
         }
      }
   ],
   "tag":"pEh6LS1GCTYQaWR-6vAe_Q",
   "iv":"ZMHYqq1xV1X81bFzzEH_iAfBcL75fznZ"
}""".stripMargin
}
