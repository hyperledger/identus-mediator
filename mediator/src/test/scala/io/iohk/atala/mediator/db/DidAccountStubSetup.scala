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
      "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImEiOlsiZGlkY29tbS92MiJdfX0"
    )

  /** bob (https://bob.did.fmgp.app/) in the new did peer format */
  val bob =
    DIDSubject(
      "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vYm9iLmRpZC5mbWdwLmFwcC8iLCJhIjpbImRpZGNvbW0vdjIiXX19"
    )

  /** trust-ping from Alice to Bob */
  val encryptedMessageAlice: Either[String, EncryptedMessage] =
    """{
      |  "ciphertext" : "1EIFz3Vc-5JJeWFLsPZXYtglzB9AeFu_KrazoUxW0aTaTDejhiZgaZoBib1ztjKPGCB7co7U76GgcAOo3kMqS7O_C3G_Kb5WsLoH8WlvUmJndrOsFaYJ2gWefjw2G9qIBWTP6vnNQ2srvn3w0tXhRf2CI9FxVYwPlN_ZAX9wQOnRSKhKWP-D7Srsqw7YRSQLg68dBNpo_Iyf-VhOs5CYgsvoY-Lo9EtEDGHb1VA-GANFIinLCATa7gKGm7G25i5ZuBdp3j_hVVFbhjOxPcBp15jgtVPNmBqSrJ7z8xEpZDmkUacR6YNQB6-aXRS4bFyBbpD3PiEjcNcrmRcea3BxJwnV3m9ye71fMsXPRFJfcm4K2yWHRXHC8WmJULK-a-Nx9c_DyeGbUjocCt97t5ZmFY_VbNTksWCcTrIo3btG3aYnJ1OwXp76QoNYQYPsO-zw-6ORUDU5EAcFmRya4tlL1gj1wz72OmylW4O2QOsmYTGXpqz6-HX17nzm2FNzW9HwxT7SeeUnogPztrnGpdV5zOaN46AU8uGXGsA-l55a2mUyA3Y8sCRPxbAAhnuvaxu3Jfkj7ChxxtAAWEB2jlWbf5i9KlDlmLLN98bCq3pHOLWIe42W4gym8T1khZDUXe4xpOEJJX5Gw0ZnCHqkk1IJ9QaEfBTCPH5T4vicLiE8AVY1gyjy7vd_Al8NyOIVmenPf-YaGU08rTXzj2T9EZBLF_0f5EYU6Uzhuk9Dr3LlQjisTNPMpq6BxolNFMN3l2FrrlzwmImU4QKJl9pU5Bsw5dRWFYgeq5g_BWCj-tMObU3QHDhNtck8wfb-Mf609vw5eGnwd8w_zY5oe04bvoU1GoxDoXIA4UWJOnbUYW4ciCyCB0_s6ZSk2sNdYqee7JozRp5b4OQ5OrA7TuKh1KBs0eEqTTwUdPFpkNOg9ex27VE",
      |  "protected" : "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6Ik1GamVvT2FXb3ExeHBYOTgzMGI5MEdPN2lDWm52X3lVSENIQXV0VFh1MjQifSwiYXB2IjoiWHRocHNxZE52dEcwUFdmLXoxZm9LNFBiT1dybW1JMEhwTGh6NDFUX2N3USIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqcDdJblZ5YVNJNkltaDBkSEE2THk5c2IyTmhiR2h2YzNRNk9EQTRNQ0lzSW1FaU9sc2laR2xrWTI5dGJTOTJNaUpkZlgwLlNleUowSWpvaVpHMGlMQ0p6SWpwN0luVnlhU0k2SW5kek9pOHZiRzlqWVd4b2IzTjBPamd3T0RBdmQzTWlMQ0poSWpwYkltUnBaR052YlcwdmRqSWlYWDE5I2tleS0xIiwiYXB1IjoiWkdsa09uQmxaWEk2TWk1RmVqWk1VMmRvZDFORk5ETTNkMjVFUlRGd2RETllObWhXUkZWUmVsTnFjMGg2YVc1d1dETllSblpOYWxKQmJUZDVMbFo2TmsxcmFHZ3haVFZEUlZsWmNUWktRbFZqVkZvMlEzQXljbUZ1UTFkU2NuWTNXV0Y0TTB4bE5FNDFPVkkyWkdRdVUyVjVTakJKYW05cFdrY3dhVXhEU25wSmFuQTNTVzVXZVdGVFNUWkpiV2d3WkVoQk5reDVPWE5pTWs1b1lrZG9kbU16VVRaUFJFRTBUVU5KYzBsdFJXbFBiSE5wV2tkc2Exa3lPWFJpVXpreVRXbEtaR1pZTUM1VFpYbEtNRWxxYjJsYVJ6QnBURU5LZWtscWNEZEpibFo1WVZOSk5rbHVaSHBQYVRoMllrYzVhbGxYZUc5aU0wNHdUMnBuZDA5RVFYWmtNMDFwVEVOS2FFbHFjR0pKYlZKd1drZE9kbUpYTUhaa2FrbHBXRmd4T1NOclpYa3RNUSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0",
      |  "recipients" : [
      |    {
      |      "encrypted_key" : "UKna2AYb5bH66LO8aATaObubaRgt3Eeu742p28-1YPl7irvY3SgopKRHJf1d3oYn0Gs2Kim_Zv5Y8CpJqCwAiPwtJ_qnSd9k",
      |      "header" : {
      |        "kid" : "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vYm9iLmRpZC5mbWdwLmFwcC8iLCJhIjpbImRpZGNvbW0vdjIiXX19#key-1"
      |      }
      |    }
      |  ],
      |  "tag" : "uRH0gB3-fGvyqMulg9L9JbIVt4AIaeBJxRYhzuyhUlo",
      |  "iv" : "G8MpH-4mTlA3s59nBmnfkw"
      |}""".stripMargin.fromJson[EncryptedMessage]

  val encryptedMessageBob: Either[String, EncryptedMessage] =
    """{
      |  "ciphertext" : "vnxtXHZRAeP3s4_vqTLyXphlqb4s_pWXs42WuqDalio62mE9gZNAO4HcYHDMYMaYs4bfAV0flwXl1rv32MIHIu2toE5EkvfR9josZYq4ocYVMx0NpGCmqyJURfScp-8oJKvYe9A7Si_P7F7yMHZuuJFxSfA0i5bSPs0GGRoK4Zc_u8WCDr3O9kvZJ7YVAt6kwobmdIncORolq_hMpXAwqlaOxkJ-irAdIAq_qbutEhk9EviDtYkCmD1z0JC9RIKx4qMwcDZrt-1dVSXqaIKt2K84jyW_--Qz_3Ifq2aeO_9Led-PRRYQUVzMMFn_6n9sOmWxqWt3BQDinNLPGu9v6CiHAL8CpemjQMbjfsocTUpHg5kqlFd1llkIchfp0t1-7E7YfMfYRRRnJ6eAlJsP3VsnAP_qghA6iNYLoD20pwvGPqA7qsXUVHC059h3SknS6FQJzwiHRO56IXKQfErFOkFqdXwxK_6swHqHWP5gcCVi87ogxQaBVEXlCWjXtalA02lS-kXag8r6SQS8A4LB4xAJrA6W3JTuZD41-zkJaaw6Lg0f6WkJxrGdBEqlQi8HRmXW06eb5HFJg7QXTuRRXgUdM4BTPyjLUoAm-I-LpvRwSVYC_xDRpI5-v8FAgHkVat2jQzXuNrHrZeSb5pb_CYpy1Nz8WLESV3rGThI366SLbmhugga_qjcLv6MpHcHAwfUZuNC68JrlDsMh7rHQ-iCN8CAKnd8xygD2zdKWTkUkWlHIJEXBnmQgo-jZPYT4cq05dm8wK_EEqNPzYF5XwYS8DoHlatdI-boPNQjAClBkJE4pAOUbFh-MJ2re36aTjmuD0SmEIm54EXJskvQLz3YszKtnC8zjkHFqQ3Dh8Rgrmx7RGQmqKPMnEg4aYSJwNsSSmvyMYw8v2-ZkW_Qe-BQbB-yxDdQL_2lAdhDGcXeyByyiYP9wkr8yuaX6iS98",
      |  "protected" : "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6InZjNi1WcG81SXRiUHcwVFRfSVMxX3plM1BWa0RlZ0ZuUWpvVGNkcWhwVTQifSwiYXB2IjoiM1ZUUW85TzUwakpOLWxBZ0JwSFFKTFYzTmVibE9uMklxSXZZS3U3UHQzbyIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTa0d5M2UyejU0dVA0VTlIeVhKWFJwYUYyeXRzblR1VmdoNlNOTm1DeUdaUVouVno2TWtqZHd2ZjloV2M2aWJabmRXOUI5N3NpOTJEU2s5aFdBaEdZQmdQOWtVRms4Wi5TZXlKMElqb2laRzBpTENKeklqcDdJblZ5YVNJNkltaDBkSEJ6T2k4dlltOWlMbVJwWkM1bWJXZHdMbUZ3Y0M4aUxDSmhJanBiSW1ScFpHTnZiVzB2ZGpJaVhYMTkja2V5LTEiLCJhcHUiOiJaR2xrT25CbFpYSTZNaTVGZWpaTVUydEhlVE5sTW5vMU5IVlFORlU1U0hsWVNsaFNjR0ZHTW5sMGMyNVVkVlpuYURaVFRrNXRRM2xIV2xGYUxsWjZOazFyYW1SM2RtWTVhRmRqTm1saVdtNWtWemxDT1RkemFUa3lSRk5yT1doWFFXaEhXVUpuVURsclZVWnJPRm91VTJWNVNqQkphbTlwV2tjd2FVeERTbnBKYW5BM1NXNVdlV0ZUU1RaSmJXZ3daRWhDZWs5cE9IWlpiVGxwVEcxU2NGcEROVzFpVjJSM1RHMUdkMk5ET0dsTVEwcG9TV3B3WWtsdFVuQmFSMDUyWWxjd2RtUnFTV2xZV0RFNUkydGxlUzB4IiwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1lbmNyeXB0ZWQranNvbiIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJhbGciOiJFQ0RILTFQVStBMjU2S1cifQ",
      |  "recipients" : [
      |    {
      |      "encrypted_key" : "CtMxsvs8c2WefiEqs83jTSVNekUhtbiddZTxRaMAc_wDgJ-ZmllZn63six7HvfkHQE25W8IyS-jCHdsaIa09xPqr4R7SzXCe",
      |      "header" : {
      |        "kid" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImEiOlsiZGlkY29tbS92MiJdfX0.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6IndzOi8vbG9jYWxob3N0OjgwODAvd3MiLCJhIjpbImRpZGNvbW0vdjIiXX19#key-1"
      |      }
      |    }
      |  ],
      |  "tag" : "PBWjww23CnAmuE-y-LxDMErye0mo5lO72Ox9d4vxzHA",
      |  "iv" : "5p68gZa0ZPaWDG7OCnI8nQ"
      |}""".stripMargin.fromJson[EncryptedMessage]

}
