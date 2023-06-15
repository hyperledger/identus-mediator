package io.iohk.atala.mediator.db

import fmgp.did.comm.EncryptedMessage
import zio.*
import zio.ExecutionStrategy.Sequential
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object MessageItemRepoSpec extends ZIOSpecDefault {
  val port = 27777
  val hostIp = "localhost"
  val encryptedMessage: Either[String, EncryptedMessage] = """{
                                                            | 	"ciphertext": "4a-gSYLNG0CWNpFCUQ8abxvIAJYBrIFc9F3ch0HonJbJWAk1BL0yQ7G2vES40q6-4CvJayDDDV04y-V9Q8O0J4dd-lio-IzI6GN9VVJ3ZlKSdtGJJA_4nS5bn0puDYqfh-QWQhtPTEO0vdKJtRZ7gVqfN9IQrkn28HhHphKu-O9WFzneOicjyUe3xigRxOY8y96QerSEGcVrewMGEGryzvZaX18_9w_ioaDlYhMBLLin2OsL6wu8a3mUOFjwFpLyXZlaItU34h92m2W3rV246UhJ8LWS0PezVJf2-lbbwo2pFQNzLwDir7YxFo67p1u0V6XyK88V24n6PFJVnJCbD_w9fgs8PdK5zEOwg7Egq-XQcA3n_hgTt7kWvnkyx-az_t-VXYmHhjo6RoakzlcExPAhwkV2QxV67tDJcP5hVKR9qv-e0swy4gjnCGNSCMGYQIGfOLqkGwmGjIEe6fkxxgsKxH4YGBuodI7tVTNTCk0JeGNcj6qy_9ypCsjaPxjp8MHKWdXb66PG55CuDzFweiY-Jc8FVwq-jZPRorCxF6m4537ecXIbaUoryf7NblbRtnxxodg-FyRiVVF5xkQIBE8qkJh9AnKaJ3Ak23TtGWHYm1pKa7rxz4Kva2VXPqVTNABElUldAx9DPA-TM-nxbkkEmyhYh72IrkCKEaVZ7iWDckla3bfPY9MKafLc67A4OuVq0lVPgrGunrlVhmsRTyyl3NpwjdcV54hBSdwUBwuFOnaOp99E5Q33e_llF5KTe4dUJFxoq1fTvS6WPyeLejivpTg5j66Pkr1swPk1jM8bZQuKKn5J2QFMqGjSdeI5",
                                                            | 	"protected": "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkhBUi1OU0RxZ3NndS1NeENXUVdCWXhsSGxzTl93OHZpeDhoc2hqVE9EWDgifSwiYXB2IjoiNXJfcEdxMkNPbmRyQUFWMDlpUGdBeS0tXzhaSFFOb1ZoOF9WWXlhX25OUSIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWhiR2xqWlM1a2FXUXVabTFuY0M1aGNIQXZJaXdpY2lJNlcxMHNJbUVpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmUSM2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eSIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJkb2QxTkZORE0zZDI1RVJURndkRE5ZTm1oV1JGVlJlbE5xYzBoNmFXNXdXRE5ZUm5aTmFsSkJiVGQ1TGxaNk5rMXJhR2d4WlRWRFJWbFpjVFpLUWxWalZGbzJRM0F5Y21GdVExZFNjblkzV1dGNE0weGxORTQxT1ZJMlpHUXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRsb1lrZHNhbHBUTld0aFYxRjFXbTB4Ym1ORE5XaGpTRUYyU1dsM2FXTnBTVFpYTVRCelNXMUZhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxFak5reFRaMmgzVTBVME16ZDNia1JGTVhCME0xZzJhRlpFVlZGNlUycHpTSHBwYm5CWU0xaEdkazFxVWtGdE4zayIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0",
                                                            | 	"recipients": [{
                                                            | 		"encrypted_key": "dNqt8Ne6lV7GP6n2k0CrRJafP7V5aTHqBhcTHi3p64kcnIHyopirWOpd_5VB8EHcF1QMK5yrwlkdu2sSKPBkLjhN7htNCTU1",
                                                            | 		"header": {
                                                            | 			"kid": "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9rOHMtaW50LmF0YWxhcHJpc20uaW8vbWVkaWF0b3IiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"
                                                            | 		}
                                                            | 	}],
                                                            | 	"tag": "wwXjvlM5qD3YrgP2Zy0CSlxerbw1b8EX5q5xiTT_8zY",
                                                            | 	"iv": "HC5gXYcgQvAVXeulefvTsQ"
                                                            | }""".stripMargin.fromJson[EncryptedMessage]
  val encryptedMessage2: Either[String, EncryptedMessage] = """{
                           |	"ciphertext": "4fPOySbR4VEOPhtK8Nh0EV_6eEG7p2IQmyQyuqC6XjQ1BroZLbUCM1eIKQZ4JO44Mop0tsDvzSkRO1LeBf1VuzVuZQU6x424xzraIwx-9hJdg17EzvRJYnVmT7nCzQXOs0XM6BEFP60ECkNzAZtD9tM5NyvIHTsAUxR0SnxVEaDFPNu8QB8PS54-w5xOai760Zjdvs5ytyra-qJM29FOhnAbDQIk0ZXLYn-zw1FwUojD4RpqFBgS0h5u8Yq8o18MeKgDQIZbMo1GDeRSjAUDW95Nw-iXiyPCf9MoAB__mgWV1diTtBF1UK3jZT55Vo7EA288rBx8a8uqHKDSaAf-Jls9Hh8OTCUHP6jTfBSh1T0uvYOoreG2OJouCjHXp0P1Eyi3DSxtOZBQYPOsD1rI8D-98DTJ29EGRZs4NcE2KtTZK-nwfLaPS5b0uYT_3CYotPj5gNBnvnnXCTlOdMT9tBy7FaVYRv_Y0r6cB6zda7GOC9gh_uOSkqUJMwkdm9V0TtohW3KKAoHIxK4CJKemaZPm_gHHdh33QsJbEPP43TinG9_9wUNX5IQXA21OChYFWmmKZxy88Efq58MNNYH_AqYqxSslWGL_pJa6hnJZbbz-s2ieT1slEsiRUkHhWDW3SDuUfYAdz_lvuErUc55IT6sLh9aKTkEQyIwHMAJO8fCEce95naD8j6eLzHvhXmSydPeIDzlKwRcJ1qUutYFo3Bj2T6vqxgHjJPgcH1x6cHYaqdnP-WrehRDLH4h8Z-UxU8W1anseLHGsyxTHXH3nJzkg7X4EEogsvqUEOWSM_Ag1LbgP1sCzvAXTox4vbS-bI1xLYCIHdbT-f-YeqN-tdP34VAp91ZrzNRmf8pSx3KHFFj7_H8vfLKCZTsp7YQkXKUNvGmFgxkt0qXneGKmv_QhUZva74nVI2yyOlRpuio6j6-jOZR2YzRKLBBcepxPwaOW8H-YMfC9ZWTI_Q9lgCKdV2W8MyJvkeXPCnR85iHwg1Dlyy2aX31dJY6fJUxzjAiEvBVeNPLCeIazwHGBGlW4SdLQGWURpP1ag8ZWEd-zIJe_07dinFzs61D5fhIg4DCeNMbBAeHTFgbwUyy3O_STH21cZzpPeXs47e3leZNs6z3D3KUUOptQFK9H8b6_RryNiSiizQuEQcgXEfoROojucMzZDSi5xtDCQErVvDGs2aKQRcSBa25NioJMluC00o91OcGfp9mUe_hSebIUyj7IwyWDGuNZyUJXiF876cNcR8uX0VtXGK4IT-ppue69aPjZlbAlj5C1ZocPzM3nrBCbMYNZLQ4ubllmeK7QHSIe2SEJN6PxwhFbuMK56Ha-7DCYX6BKHjFgM0-Vdw470SzsacFWlYRMRJeepcqaCRiV_35FlkEGvM1lrY7Bng2kaQA3rUUUXMRX12UG786IaJ9noQjJFzW6uiPlS3snICwg1g6FDeeDCubvtRL1_DnnE2oEkcvtHnfes6rhiCho6cGTWjur-2ULCRRAmzP9yH0qAb99e9cFU7fuGYZOMWXXkS7Pp8QDRK3wIvRvSOitnpJ6lbAmERAcNxYvBhebiAQmNzkGpxlwkPHk7t8gZELSCSjr2skFCDRcjBdOT_fdBTUpEir-RpzsLWgo_WpO5HO7N2LdP6tuY4ZWwpC6fab8pfTHCwyR-EhygFxRw43Gd4taUetJOWhTVygAOKmpNkhPOSMbRAz9kmNNmd55TwoZPxVEnOOJhTmTFHzVSJlyBsyPK3OcfM5vZ2avl_9en_2UaqiQemficOarxP30W871pwxey5v5gztJDvELqiR9F04BPFz3FpE65cktCRVPvfwdiDZkJOCjxlNDA-oZCAo3cfun5um4eioJGwh6B8lenpSnYufYOOrxXr7E-j27pkNnq390UWrP0ciz15WEj0s7UZXv4TgiqIWv_SVWeVfxr-lhYIR7gfaW8PBABfKmW1cxWJKLmRBuHrg_gVwaLqGVtriS2_cFYJE10N1DJDBXfjyqhVgT_h9Vuyn1NNaculPwLETXJG1ejwJkE-4XO0uvI1EuRVqKHwBPzL88rPO8g8OvN3zxrQ-cBIjF2cdlOyXaaYJnjy2K30PdgWvZt-To10XYGXinczMjEg9K3LQM_RjYSMl_eE_3bBpcQbWzc2jX-xRPWIhlVF0nxHQ2MIMuHldvR-NDUsbuKJhdoy1WunSGomhLI15Wb8SAd4MMvxUsx6KwiLSrgHaerYcBSVWcN5YTc9H82CZSlanLu6cjReLnWZoDdYBDgm_CxeimorX0TXlird4L3MyYR9O61Vq8rpozVPNat_hpNBi3Ro3QHVRtsdbz3GuTdNdhxrHaqDNOxG3lCKFrY2iTzZZPAuPnmmFIcGpWDbQLnOQ64YjNBtpgKHdKmeOW_kTu3nNCuLP6hgr4XmGe6san4BuqcGrJWHqghdd6JLyef1vZotjB3DoY3uuAYKjjcqtW-gLVj9PdOFaxPNVeTM91MOrMO-sMcCqwWBh-D0aGHWb_DeeR8UW5VXcTd9wBPzNLPgtiQm803WRJWrs6SY-0tRmbYbtS3GN5tGDQfziCPjl6q2e93eBZuPMe1JEHtVs_BWGukOc6JDanQNhhgGLFEgs_enTpWTlKf0BQ7COpHTDBomFApy_M2d8d_SUI51o6dT_mg8rZ7zYaeCDfLPKXrZbaT2dZ1eUxKQrvKRHESbhRq_S7qUsHZy13rYWehyxm6Imm1LOxJzZOtwlfw7GF03Ruo7rTEXbj86h4Hnu7eV3KA1uXITe7ZQx_XKflHgGXizbDj_P2zl734HJMpORmx_v4h0aQSXiYokhWN6Q-RZFhIHKbKElHKQNwFLNxWmqpISDJxkDKfPtuvbIl8p2Zwvk3tLO4-dJwodzP2i6necSIXYc_Tf6rhEhqohfeMnzeWIz-CR8PwI6N0_Z4Q0p3aUt_2iwYoxf7J-eFDm3DM-QYCgv_7OgF9uYW1ghUTaui2boiI5gr63a5RHkbyxkmsUfTRGEuYicXLYTkQ4H5b_osXsMZsC3roR-xL5JYJjAdBoV7vW6vXJbSNkRmbc7e7SvtvblnNBJHCPaeDaclinv2HalQNh0O7H5YUAcTUgjKdeobQqrjNcsCkPsPxE6NcbHNU91a-y5nOLKxjXIvfED8KoBDJ8KunnDhOa4fzjlkw-VJa_vcesc1XmsqGglkyBKPr_nZvV7oZVrx0gI-V7xKWOr9QQKD80H1i9F7KTk0icBvp051RPSMmsg-CYPFOJQBVQvgTKNn-Fsk-dlIlBSdF8LtHyb5ui4_AWZkm9jNjFtaMRizuUr_xSZWgUdU-E7IhicQsXN62Ok9pNwoJcO0qB73H6E7ZZskS0H8_ihvh-fIqnC6suUvQ7oWzPeqHWLpFOZhGcuOM4tF3o9eGo_sQqy0HcG7x3j-ZRWDXc36GY94QKnUfB5M7jCFigR_Rtd2HMPpXf3icOJIZOEdhWV_42RN8Kz3AYzSpu6U3fsyfIacTwsXRFtS8WLgNK9iagmzvDagfMBxthP0mkfYyg6rU5bR3x8i9FVSNLqzcQu7tUi-ZHvIscTWV5jZU8HLfq7ZnqBLVgPrp3zc5r57yIBTmcrhzlSq4D7Dkv1yb8G4FsKG9_C9Y_eqDmsKMzTrUsBZ6sE3YHPuTnh8YoYsKOMMm6XrIkxJ7WexVeiA3cFziqAooZ4ILkXOJmUZ5SJ9PGhAF9F90ZaMBzMDvhxaxhqFBSilUYB_C9wfKvRcv4bMro8OZr5f7S5GbAEvl0mmIbtZdS-oF9qmxXyEQyKea4yLjfhIiUSkaM3fVmweUfOL3sJXjrTc7auH_GKxpWJcpbrlc-4M1VbbykH80I33Apg0AJzHzlixawa4NTne0Te_agAYhg3cHcpRLA4rHGu5ADLplRdu4vupEjTQ8eZtljJcGcKQ9pd08gZBNQj7JhYBC18fTZdPptJEPXy28U4f1l4CL5HFKH0x_ekYhLHZoocD1Kon13ICX3oT12EbJbuPj46zyUba9jHsFc1fH_jcVxTdJDX--WIbn-bSqG7lA3Yq8NBddbwb6Y4kGCMSWTNhvzg0lKG5fLvgt24Uk5RjDpWdclFvlFMUmsy4H_kpyJcnU47YRYfjj63YNGaXaCCYKAdmTZYeNsDDRvbA1NkjZUXSZZZqpkDftiEV39Eap1Yd5POj_Bg",
                           |	"protected": "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkxPOGRZZkM0TTk3bW4wWWRnV1pPR1FTS0dGU1d4YXRqZ0RwaGdWc1JGZzAifSwiYXB2IjoiNXJfcEdxMkNPbmRyQUFWMDlpUGdBeS0tXzhaSFFOb1ZoOF9WWXlhX25OUSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC1FUytBMjU2S1cifQ",
                           |	"recipients": [{
                           |		"encrypted_key": "FqF3wyvfmdX1Iw-0IGgLRl9inTg9Q9RwEpFVrkBqwkAq9QwM1bo2f-g2vyDOflNWp9-uCCV_jcepYe3rkNukw8yzLSjD5Gow",
                           |		"header": {
                           |			"kid": "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9rOHMtaW50LmF0YWxhcHJpc20uaW8vbWVkaWF0b3IiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"
                           |		}
                           |	}],
                           |	"tag": "0A1FVgykF0bzpWMhuptzsC2xlau5ARpo8eHiLh7CtHc",
                           |	"iv": "GqThxkzvZTM4MH_NLgjZng"
                           |}""".stripMargin.fromJson[EncryptedMessage]

  val connectionString = s"mongodb://$hostIp:$port/messages"

  override def spec = suite("MessageItemSpec")(
    test("insert message") {
      for {
        messageItem <- ZIO.service[MessageItemRepo]
        msg <- ZIO.fromEither(encryptedMessage)
        result <- messageItem.insert(MessageItem(msg))
      } yield {
        println(result)
        assertTrue(result.writeErrors == Nil)
        assertTrue(result.n == 1)
      }
    },
    test("findById  message") {
      for {
        messageItem <- ZIO.service[MessageItemRepo]
        msg <- ZIO.fromEither(encryptedMessage)
        result <- messageItem.findById(msg.hashCode())
      } yield {
        assertTrue(result.contains(MessageItem(msg)))
      }
    },
    test("findByIds messages") {
      for {
        messageItem <- ZIO.service[MessageItemRepo]
        msg <- ZIO.fromEither(encryptedMessage)
        msg2 <- ZIO.fromEither(encryptedMessage2)
        msg2Added <- messageItem.insert(MessageItem(msg2))
        result <- messageItem.findByIds(Seq(msg.hashCode(), msg2.hashCode()))
      } yield {
        assertTrue(result.contains(MessageItem(msg)))
        assertTrue(result.contains(MessageItem(msg2)))
      }
    }
  ).provideLayerShared(
    EmbeddedMongoDBInstance.layer(port, hostIp)
      >>> AsyncDriverResource.layer
      >>> ReactiveMongoApi.layer(connectionString)
      >>> MessageItemRepo.layer
  ) @@ TestAspect.sequential

}
