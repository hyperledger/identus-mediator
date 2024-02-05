package io.iohk.atala.mediator.db
import fmgp.did.DIDSubject
import fmgp.did.comm.{EncryptedMessage, PlaintextMessage}
import fmgp.did.method.peer.{DIDPeer2, DIDPeerServiceEncoded}
//import io.iohk.atala.mediator.db.AgentStub.{keyAgreement, keyAuthentication}
import zio.json.*
trait DidAccountStubSetup {

  /** alice localhost 8080 (http + ws) in the new did peer format */
  val alice =
    DIDSubject(
      // "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
      "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ"
    )

  /** bob (https://bob.did.fmgp.app/) in the new did peer format */
  val bob =
    DIDSubject(
      "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vYm9iLmRpZC5mbWdwLmFwcC8iLCJhY2NlcHQiOlsiZGlkY29tbS92MiJdfX0"
    )

  /** trust-ping from Alice to Bob */
  val encryptedMessageAlice: Either[String, EncryptedMessage] =
    """{
      |  "ciphertext" : "kz9lDUV-hrWKTVQvLfMIhCvJx6IZj_vnAmc9OGdmS_sg6w2shVC3d6a5Ox4H-7VJJuDZdwnP9e45cGVdJs0mzy_v2qx_UCQP29XMxgZLEUn7sgfs-fSCs7NM5CrCRpfc2gO-ukdBeP2IQdJ8QNrM0kXv-z8B-PwUv7Q7AyIhjIsXXjtEbwYJCGMLqdD5U-lCvDu5WLKqG0HY57TQRU8Cms32UvUehmYavYR_rJFH6WN9Hi6UUln2aAfsbjvsbM0gLAOWm71zPMeMIU0MkwKVdEDsXicseoXHD6Xz5aVwBZ6iS88t4YBPOT4q6mPRIf1WY7e88mwfRb2X-Y0nsK4zQhhZKUd9Oi5yJ4wndjk7ILfoqWuYHn249kzilmSWqxWjlDlziH46twEHFex_l6Cwdvbcd6GFgr0K0NrAhwMULdKhSBdJ0VG1V-YPze5o20p-oX67f9iZlZ2dJA1_ND9BEtDeFhDomcQxAgl5_xW17eyDP4N-oGCaI_0l_CjTFJvGB-Xb7AI0EDKe0HSLnpiJYnDxySd9dD-lrFeiKnyy8paef9hvieh5jlxUQHUZJqjbvg_Upn-I-HXt8NBQTr-fK2r3nZO6bmVV_5kux8eiGQCt6Z-ictQefkhy80xkD-xlGA5Bdkmb_R0A_RQqP-OTju2pfewKp-Pi7Z2uCB-ahMbqnPHJhuaOXX8MR7ajmXHGUFNqbDR6rOWq9U8HyF9NsF1Yi1XkHqQsQrU7A2UBf64y7VYfodyzRxaRST2RhKQhIdAmqre1da3fyvrWp4ti4gXHhoIDXN7msSmYI9L_7k1iPa-DHMn-iafIACQwq34h_zcTOGfJCClX8dvp9f6LzxglOTzNoLrTzTtEk_vUa6T8KeMlLRHbabzb2WTTNnDieE1fA-mibhlLO7vR-v56n8x38DJUEIembltDKTsSnPU",
      |  "protected" : "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6InZEMm4wWExQTXVvY3k2cWlvQUZTdjYzZ1luV1pPU3ZJZW9DTEFKd1RheU0ifSwiYXB2IjoiNlFBNWFGc1B2aGJRUTBQUzdMeWJzSmFaSVNiMlE0Mkh3MFhaLTNONGVaQSIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqcDdJblZ5YVNJNkltaDBkSEE2THk5c2IyTmhiR2h2YzNRNk9EQTRNQ0lzSW1GalkyVndkQ0k2V3lKa2FXUmpiMjF0TDNZeUlsMTlmUS5TZXlKMElqb2laRzBpTENKeklqcDdJblZ5YVNJNkluZHpPaTh2Ykc5allXeG9iM04wT2pnd09EQXZkM01pTENKaFkyTmxjSFFpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmWDAja2V5LTEiLCJhcHUiOiJaR2xrT25CbFpYSTZNaTVGZWpaTVUyZG9kMU5GTkRNM2QyNUVSVEZ3ZEROWU5taFdSRlZSZWxOcWMwaDZhVzV3V0ROWVJuWk5hbEpCYlRkNUxsWjZOazFyYUdneFpUVkRSVmxaY1RaS1FsVmpWRm8yUTNBeWNtRnVRMWRTY25ZM1dXRjRNMHhsTkU0MU9WSTJaR1F1VTJWNVNqQkphbTlwV2tjd2FVeERTbnBKYW5BM1NXNVdlV0ZUU1RaSmJXZ3daRWhCTmt4NU9YTmlNazVvWWtkb2RtTXpVVFpQUkVFMFRVTkpjMGx0Um1wWk1sWjNaRU5KTmxkNVNtdGhWMUpxWWpJeGRFd3pXWGxKYkRFNVpsRXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbkEzU1c1V2VXRlRTVFpKYm1SNlQyazRkbUpIT1dwWlYzaHZZak5PTUU5cVozZFBSRUYyWkROTmFVeERTbWhaTWs1c1kwaFJhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxnd0kydGxlUzB4IiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ",
      |  "recipients" : [
      |    {
      |      "encrypted_key" : "pAjHJ8WNs-tiEHhqyr-CebCiN2-dGR38AOeb5cX3YLCyOYoWU6gmnbpbqcG5isDjT6ACH4DHz7qrSsfWCdjEVQ6AqXCfs10T",
      |      "header" : {
      |        "kid" : "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vYm9iLmRpZC5mbWdwLmFwcC8iLCJhY2NlcHQiOlsiZGlkY29tbS92MiJdfX0#key-1"
      |      }
      |    }
      |  ],
      |  "tag" : "XIBPc7lj5kei7kAxRTkOc7KO3gQVzs3MPPtDbljsQOo",
      |  "iv" : "8NWSlttH8-2cCIIZJgZleA"
      |}""".stripMargin.fromJson[EncryptedMessage]

  val encryptedMessageBob: Either[String, EncryptedMessage] =
    """{
      |  "ciphertext" : "lahbV-XQWDkAOMC5S5MWjkkOn8uLgAHiQSUu-J9xRStI423jhlLQ7rF0mS1QVxEISDAGjas1f3xi12T7-BSyPWC87IlaAZLYaGB9SzVIooZb0Ciaw76UvrZQWzJc3K41PPeQ7zJMXriGj9FXdmbNBYWkfhv9mjUhV9SSDUvwV3JyvvAREubGQ1Ol6Oo6o_e2_fq-WTr4-p_k9Yp-cDcvZajSuVjm1Y4xfqDhEh5d6D5_HlOfFUWaruZ0r5JbYnjJqqHAvYvAhhU1IsELUV1-qk4YdXQL6vNG7WItFgQzwkAoGTbt4zWMY5zrYY5WeixFoqCQ5xATtpZKDccCQd7fZNk0R-KdADDy5fWecCQw9YSfyCdAIo7Ui1jUW3s8k9-O1oU0RJC2lpTRzCvu08Pqyp9zJLOO-0hJC3fyWYOXc6F66fHkmDQoDZNrJEzFfAnTV5w9eCYWDKOcnaUX1WmnxiABtoON_bUxYgxDsRIPa8mEV1H3QJvLMEawwTkKvkLPcP-tNgYtIOiCAmCKF55JI9UbZketgrk4Mrs-T6u4PtUlB4zrDBiqxawqGTUZhRIrrHFnBzLqPYQmyCFJ_CEL_1FvfBrkMVdp0zl0z91kq5IOu2guYvFwQ_3beBqjOpkCDAvw0XG5MdJDBfgZnOOWAivaMxE0uze_msax2hot37ejsfG49UYOUtzcDCKBYhRI4UPp7IDK8oSmcDp54Tj1m5peh8Muqh8RJNyLPcntw2F8SoQLMRyPkJ_kMi9UcCSfBiWN2zRdk_rxOylp_ksodNRKo8IQ2F4dx_P8pCdYy4m8yIttF5GSd9I_il4LjvsKa6EeL6GOqbryDdV1qZfiEvGMX6ox7p8wbBoZmrbxz2urt7cihb1gpeXzQ_Of4ijl4jVLn-Pfs6bldJPlU_HP8UUBIujAs1mMLJjEhEZawrCIqeT2oV4S4PtDV1xqTuf9",
      |  "protected" : "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6InVOeElyWTd5QzJHa3lvRTdBTnpmTFNfbVc2MnNqNVN6dFd6ZVV0V1Y0eEUifSwiYXB2IjoibWRTMl9hQ01SWE9tZFFwSnF0czdpWmRZOWdXbWdVV3BIbmJaVWg4OXkxQSIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTa0d5M2UyejU0dVA0VTlIeVhKWFJwYUYyeXRzblR1VmdoNlNOTm1DeUdaUVouVno2TWtqZHd2ZjloV2M2aWJabmRXOUI5N3NpOTJEU2s5aFdBaEdZQmdQOWtVRms4Wi5TZXlKMElqb2laRzBpTENKeklqcDdJblZ5YVNJNkltaDBkSEJ6T2k4dlltOWlMbVJwWkM1bWJXZHdMbUZ3Y0M4aUxDSmhZMk5sY0hRaU9sc2laR2xrWTI5dGJTOTJNaUpkZlgwI2tleS0xIiwiYXB1IjoiWkdsa09uQmxaWEk2TWk1RmVqWk1VMnRIZVRObE1ubzFOSFZRTkZVNVNIbFlTbGhTY0dGR01ubDBjMjVVZFZabmFEWlRUazV0UTNsSFdsRmFMbFo2TmsxcmFtUjNkbVk1YUZkak5tbGlXbTVrVnpsQ09UZHphVGt5UkZOck9XaFhRV2hIV1VKblVEbHJWVVpyT0ZvdVUyVjVTakJKYW05cFdrY3dhVXhEU25wSmFuQTNTVzVXZVdGVFNUWkpiV2d3WkVoQ2VrOXBPSFpaYlRscFRHMVNjRnBETlcxaVYyUjNURzFHZDJORE9HbE1RMHBvV1RKT2JHTklVV2xQYkhOcFdrZHNhMWt5T1hSaVV6a3lUV2xLWkdaWU1DTnJaWGt0TVEiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLWVuY3J5cHRlZCtqc29uIiwiZW5jIjoiQTI1NkNCQy1IUzUxMiIsImFsZyI6IkVDREgtMVBVK0EyNTZLVyJ9",
      |  "recipients" : [
      |    {
      |      "encrypted_key" : "jb8B7Gf_0kQTlvoRJrYJpUzQgYjGUvdheVof0LVP820uXWg9jD814uy-JM-pZ_AeVq8klGxVKsnrw4ft7p6QhCZdPR-0UBaQ",
      |      "header" : {
      |        "kid" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6IndzOi8vbG9jYWxob3N0OjgwODAvd3MiLCJhY2NlcHQiOlsiZGlkY29tbS92MiJdfX0#key-1"
      |      }
      |    }
      |  ],
      |  "tag" : "0VIzpDzE35C4ZzhoTvkRCl4FKktfZlinMGdg4AJRJtM",
      |  "iv" : "g12dLrB50zLYGmvBX6WN8w"
      |}""".stripMargin.fromJson[EncryptedMessage]

}
