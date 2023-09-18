package io.iohk.atala.mediator.db
import fmgp.did.comm.{EncryptedMessage, PlaintextMessage}
import fmgp.did.method.peer.{DIDPeer2, DIDPeerServiceEncoded}
//import io.iohk.atala.mediator.db.AgentStub.{keyAgreement, keyAuthentication}
import zio.json.*
trait DidAccountStubSetup {
  val alice =
    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
  val bob =
    "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9ib2IuZGlkLmZtZ3AuYXBwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"

  val encryptedMessageAlice: Either[String, EncryptedMessage] = """{
                                                             |	"ciphertext": "L8wzawffzL7kbVyDcMiHgQMwlqLUqWDYOwH4f0VOFK7sJL8vO-H_JBDCADu7gvH1MDHdVJDXQmSv5dDXp6JYubagcSfBqyZGKGOQDQz-1Ir6-sqZ9K5xRPMt76dYbSswrxhaROVJvtAXyrTmN3KEv8SgP_vt8za5QU6B8cM6gp2CycJKoIhETnhVEVjHnrX0YcyzBsLhd-ZKb8e1Nlz_XSh-3cw1oIQMFjXHnU3PSjrqXcrC-4qSGpVVpHvMSOF3o2EbLGEpP-8UbI0psgSLd9a2VUF9xV55r7Bvn_zJp6tC3-KZKNLO0Rc2K-6JRgyyZjo7O_8aj7Ns6Lx0OjUXoKLiA6B79goHiv8qxJ04k5EWD_h8gt4M3_oByH_cVXH35RDK5SwNV6cVl6zRqiHUR9Ilivh6kfOLGqM2sXCKpXE78qRgHOBeOtl08JHFQO5oFkt1ob8iDqczX0nu-qwlckiibnPK1VvhFnmgLyc9lIUsi_xlCNKqIBZCKoi03xrjNobcM1dWFG7yE04nT-sSiakRNwVHBmNCyA5JhEFQ92d1xpXFGM1ojtiHCPkN5nqe7lMYVM2r7QFnN1xTHwaDWddKprW3vkz_RP7tpoPlWk6X8rLoYUvYc3MqNdbj91QMlho5rU472EX3gprIDeNV7VQiKWoAksFe1hdt62zLH8mJJjUZ3lyq4YjOmrqg7g6RArUWC6KbPgmnuJCqwigpjlwRUCBTPISzaZETAisyluIyuMW8QlRCSQdWnfPZAU2fpLBcviMODMzZTULECvRBef05Fvtd_xRCCbpKpDkGxAY",
                                                             |	"protected": "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6Ikp2TXhvYXZJaUxaUWVrNDA2eXNtWThQOGpkZGFHSjVIaVNRR0ltWHZWQ2MifSwiYXB2IjoiLWNOQ3l0eFVrSHpSRE5SckV2Vm05S0VmZzhZcUtQVnVVcVg1a0VLbU9yMCIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTa0d5M2UyejU0dVA0VTlIeVhKWFJwYUYyeXRzblR1VmdoNlNOTm1DeUdaUVouVno2TWtqZHd2ZjloV2M2aWJabmRXOUI5N3NpOTJEU2s5aFdBaEdZQmdQOWtVRms4Wi5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWliMkl1Wkdsa0xtWnRaM0F1WVhCd0x5SXNJbklpT2x0ZExDSmhJanBiSW1ScFpHTnZiVzB2ZGpJaVhYMCM2TFNrR3kzZTJ6NTR1UDRVOUh5WEpYUnBhRjJ5dHNuVHVWZ2g2U05ObUN5R1pRWiIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJ0SGVUTmxNbm8xTkhWUU5GVTVTSGxZU2xoU2NHRkdNbmwwYzI1VWRWWm5hRFpUVGs1dFEzbEhXbEZhTGxaNk5rMXJhbVIzZG1ZNWFGZGpObWxpV201a1Z6bENPVGR6YVRreVJGTnJPV2hYUVdoSFdVSm5VRGxyVlVack9Gb3VVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRscFlqSkpkVnBIYkd0TWJWcDBXak5CZFZsWVFuZE1lVWx6U1c1SmFVOXNkR1JNUTBwb1NXcHdZa2x0VW5CYVIwNTJZbGN3ZG1ScVNXbFlXREFqTmt4VGEwZDVNMlV5ZWpVMGRWQTBWVGxJZVZoS1dGSndZVVl5ZVhSemJsUjFWbWRvTmxOT1RtMURlVWRhVVZvIiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ",
                                                             |	"recipients": [{
                                                             |		"encrypted_key": "eFHcJkoUle7ZkksvZjvhJHLagF7y5B5gDQX11tc1kus2xa3Vn_QmzyVwFzScOHTCEjWRUe7r63rHBFBw0El2ukZW2tcitxr8",
                                                             |		"header": {
                                                             |			"kid": "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"
                                                             |		}
                                                             |	}],
                                                             |	"tag": "6MUE-lIZFVRId9qX-0If18EsPcfld_a3r2gQXXq4ms4",
                                                             |	"iv": "plEK7sqstAIkIxVKyfuuYg"
                                                             |}""".stripMargin.fromJson[EncryptedMessage]
  val encryptedMessageBob: Either[String, EncryptedMessage] = """{
                                                                |	"ciphertext": "-dyjeU0aRQ8F87yAJFbOmmuon0UtEhQpmDQhYclMiTAqi06wH2bLTjtJNIOFG9sJ12uIyaQahPzCFBUyR2X22Oyl-1-Un0LOCAoRP584JuN19NdgKIytmSm4pg4VeSJ56SKO5D3Qe3G8XQa_7vO8O9-b_6V9mQXXZDkknNbphuiD7S53ss_VJzbgQVLRxkBrbnU1PL7yC6AhkAZNkUNCS8GGYbdEKFeRsdWjoROEnuQ1zzPN28eO3T4Wmt2APQ10vHum3Bl1RTiPRGCx3C-fSi05F-521jyP0rwyLeMZdBLruDz2IrUudtimFI3CKQ8_BcmuXTlLa1vw2hKlhk1_ppAmggW-qrjyZPiC9-XIN02OxZox5Xzba-cmg05Dj2zmEXryrxqFfyJlrV5FKSvWDfqeNAA9QNE0aUsgpKK9gOmu5pJRCuYNL96lbxdSCR4BiuX1G_ma2gagdIIY-70d22UPGcrZTw7Blet574i5U_bIDpZw4Az0UxoKNDbCDPOpn-Shojo5n_5FFbaPky0H0N99-9cx-Ns-3NMQecpupPQ9QJ-A2KWBTLYrJofLCBWv_ysqJ3Cde9fQ3xi5DTB13dhbxKVCgzdHKlwnqPC0C3mjFs_eZ1jnzBiUAKLvD_aWMwqx7y8GUhSfXlX3hImqtXaKBu2Koc1esUOvQwadHUj5pSmsNKJJcQLzsOcuYz8U9peRevrft3sy7fm9Ne2L3SSsoCFJP1N8U8bRLmyhY0CDuCNlvXxy_VFoSdPms40d1BNZObhgwO9Ca8kp1r0ITl22repvpMQYcQ83m9fkTXg",
                                                                |	"protected": "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6ImNKRzZVeVJRbnhOM2RCa3ZQcndmZXZabFBwRzA0SzFyTzZHTjdnQkNyVFUifSwiYXB2IjoiLWNOQ3l0eFVrSHpSRE5SckV2Vm05S0VmZzhZcUtQVnVVcVg1a0VLbU9yMCIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTa0d5M2UyejU0dVA0VTlIeVhKWFJwYUYyeXRzblR1VmdoNlNOTm1DeUdaUVouVno2TWtqZHd2ZjloV2M2aWJabmRXOUI5N3NpOTJEU2s5aFdBaEdZQmdQOWtVRms4Wi5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWliMkl1Wkdsa0xtWnRaM0F1WVhCd0x5SXNJbklpT2x0ZExDSmhJanBiSW1ScFpHTnZiVzB2ZGpJaVhYMCM2TFNrR3kzZTJ6NTR1UDRVOUh5WEpYUnBhRjJ5dHNuVHVWZ2g2U05ObUN5R1pRWiIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJ0SGVUTmxNbm8xTkhWUU5GVTVTSGxZU2xoU2NHRkdNbmwwYzI1VWRWWm5hRFpUVGs1dFEzbEhXbEZhTGxaNk5rMXJhbVIzZG1ZNWFGZGpObWxpV201a1Z6bENPVGR6YVRreVJGTnJPV2hYUVdoSFdVSm5VRGxyVlVack9Gb3VVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRscFlqSkpkVnBIYkd0TWJWcDBXak5CZFZsWVFuZE1lVWx6U1c1SmFVOXNkR1JNUTBwb1NXcHdZa2x0VW5CYVIwNTJZbGN3ZG1ScVNXbFlXREFqTmt4VGEwZDVNMlV5ZWpVMGRWQTBWVGxJZVZoS1dGSndZVVl5ZVhSemJsUjFWbWRvTmxOT1RtMURlVWRhVVZvIiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ",
                                                                |	"recipients": [{
                                                                |		"encrypted_key": "KCjv83os8FnpVaIUgcuYV-5TIw4VYuGIoHozQ0JVlIwK2sEkxwWg10yh0UlbbcnNVfF_OBr4OkJUczj7pB0spRhvjoDHBM_s",
                                                                |		"header": {
                                                                |			"kid": "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"
                                                                |		}
                                                                |	}],
                                                                |	"tag": "iR0meUd8R3X5dleMywNHq5NaGJ4g0h2tok414SJ7UGI",
                                                                |	"iv": "T-I0b4fIktFXVhAIFoSmQg"
                                                                |}""".stripMargin.fromJson[EncryptedMessage]

}
