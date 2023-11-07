package io.iohk.atala.mediator.protocols

import fmgp.did.comm.{EncryptedMessage, PlaintextMessage}
import io.iohk.atala.mediator.db.UserAccountRepo
import reactivemongo.api.bson.BSONDocument
import zio.ZIO
import zio.json.*
import fmgp.did.comm
import reactivemongo.api.indexes.{Index, IndexType}
import fmgp.did.DIDSubject
trait MessageSetup {

  val index = Index(
    key = Seq("alias" -> IndexType.Ascending),
    name = Some("alias_did"),
    unique = true,
    background = true,
    partialFilter = Some(BSONDocument("alias.0" -> BSONDocument("$exists" -> true)))
  )
  def setupAndClean = {
    for {
      userAccount <- ZIO.service[UserAccountRepo]
      col <- userAccount.collection
      _ <- ZIO.fromFuture { implicit ec =>
        col.indexesManager.create(index)
      }
      _ <- ZIO.fromFuture { implicit ec =>
        col.delete.one(BSONDocument())
      }
    } yield {}
  }

  val mediatorDid = DIDSubject(
    "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9ib2IuZGlkLmZtZ3AuYXBwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"
  )

  val plaintextForwardNotEnrolledDidMessage: Either[String, PlaintextMessage] =
    """{
     |  "id" : "c8f9712a-fdad-45d0-81d9-c610daaa9285",
     |  "type" : "https://didcomm.org/routing/2.0/forward",
     |  "to" : [
     |    "did:example:bob"
     |  ],
     |  "expires_time" : 987654321,
     |  "body" : {
     |    "next" : "did:example:bob"
     |  },
     |  "attachments" : [
     |    {
     |      "data" : {
     |        "json" : {
     |          "ciphertext" : "KWS7gJU7TbyJlcT9dPkCw-ohNigGaHSukR9MUqFM0THbCTCNkY-g5tahBFyszlKIKXs7qOtqzYyWbPou2q77XlAeYs93IhF6NvaIjyNqYklvj-OtJt9W2Pj5CLOMdsR0C30wchGoXd6wEQZY4ttbzpxYznqPmJ0b9KW6ZP-l4_DSRYe9B-1oSWMNmqMPwluKbtguC-riy356Xbu2C9ShfWmpmjz1HyJWQhZfczuwkWWlE63g26FMskIZZd_jGpEhPFHKUXCFwbuiw_Iy3R0BIzmXXdK_w7PZMMPbaxssl2UeJmLQgCAP8j8TukxV96EKa6rGgULvlo7qibjJqsS5j03bnbxkuxwbfyu3OxwgVzFWlyHbUH6p",
     |          "protected" : "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkpIanNtSVJaQWFCMHpSR193TlhMVjJyUGdnRjAwaGRIYlc1cmo4ZzBJMjQifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0",
     |          "recipients" : [
     |            {
     |              "encrypted_key" : "3n1olyBR3nY7ZGAprOx-b7wYAKza6cvOYjNwVg3miTnbLwPP_FmE1A",
     |              "header" : {
     |                "kid" : "did:example:bob#key-x25519-1"
     |              }
     |            },
     |            {
     |              "encrypted_key" : "j5eSzn3kCrIkhQAWPnEwrFPMW6hG0zF_y37gUvvc5gvlzsuNX4hXrQ",
     |              "header" : {
     |                "kid" : "did:example:bob#key-x25519-2"
     |              }
     |            },
     |            {
     |              "encrypted_key" : "TEWlqlq-ao7Lbynf0oZYhxs7ZB39SUWBCK4qjqQqfeItfwmNyDm73A",
     |              "header" : {
     |                "kid" : "did:example:bob#key-x25519-3"
     |              }
     |            }
     |          ],
     |          "tag" : "6ylC_iAs4JvDQzXeY6MuYQ",
     |          "iv" : "ESpmcyGiZpRjc5urDela21TOOTW8Wqd1"
     |        }
     |      }
     |    }
     |  ],
     |  "typ" : "application/didcomm-plain+json"
     |}""".stripMargin.fromJson[PlaintextMessage]

  val plaintextForwardEnrolledDidMessage: Either[String, PlaintextMessage] =
    """{
      |  "id" : "5fbd421d-a2d6-458e-a232-396411b7a793",
      |  "type" : "https://didcomm.org/routing/2.0/forward",
      |  "to" : [
      |    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
      |  ],
      |  "expires_time" : 987654321,
      |  "body" : {
      |    "next" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
      |  },
      |  "attachments" : [
      |    {
      |      "data" : {
      |        "json" : {
      |          "ciphertext" : "KWS7gJU7TbyJlcT9dPkCw-ohNigGaHSukR9MUqFM0THbCTCNkY-g5tahBFyszlKIKXs7qOtqzYyWbPou2q77XlAeYs93IhF6NvaIjyNqYklvj-OtJt9W2Pj5CLOMdsR0C30wchGoXd6wEQZY4ttbzpxYznqPmJ0b9KW6ZP-l4_DSRYe9B-1oSWMNmqMPwluKbtguC-riy356Xbu2C9ShfWmpmjz1HyJWQhZfczuwkWWlE63g26FMskIZZd_jGpEhPFHKUXCFwbuiw_Iy3R0BIzmXXdK_w7PZMMPbaxssl2UeJmLQgCAP8j8TukxV96EKa6rGgULvlo7qibjJqsS5j03bnbxkuxwbfyu3OxwgVzFWlyHbUH6p",
      |          "protected" : "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkpIanNtSVJaQWFCMHpSR193TlhMVjJyUGdnRjAwaGRIYlc1cmo4ZzBJMjQifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0",
      |          "recipients" : [
      |            {
      |              "encrypted_key" : "3n1olyBR3nY7ZGAprOx-b7wYAKza6cvOYjNwVg3miTnbLwPP_FmE1A",
      |              "header" : {
      |                "kid" : "did:example:bob#key-x25519-1"
      |              }
      |            },
      |            {
      |              "encrypted_key" : "j5eSzn3kCrIkhQAWPnEwrFPMW6hG0zF_y37gUvvc5gvlzsuNX4hXrQ",
      |              "header" : {
      |                "kid" : "did:example:bob#key-x25519-2"
      |              }
      |            },
      |            {
      |              "encrypted_key" : "TEWlqlq-ao7Lbynf0oZYhxs7ZB39SUWBCK4qjqQqfeItfwmNyDm73A",
      |              "header" : {
      |                "kid" : "did:example:bob#key-x25519-3"
      |              }
      |            }
      |          ],
      |          "tag" : "6ylC_iAs4JvDQzXeY6MuYQ",
      |          "iv" : "ESpmcyGiZpRjc5urDela21TOOTW8Wqd1"
      |        }
      |      }
      |    }
      |  ],
      |  "typ" : "application/didcomm-plain+json"
      |}
      |""".stripMargin.fromJson[PlaintextMessage]

  val plaintextMediationRequestMessage = (didFrom: String, mediatorDid: String) => s"""{
      |  "id" : "17f9f122-f762-4ba8-9011-39b9e7efb177",
      |  "type" : "https://didcomm.org/coordinate-mediation/2.0/mediate-request",
      |  "to" : [
      |     "$mediatorDid"
      |  ],
      |  "from" : "$didFrom",
      |  "body" : {},
      |  "return_route" : "all",
      |  "typ" : "application/didcomm-plain+json"
      |}""".stripMargin.fromJson[PlaintextMessage]

  val plaintextDiscoverFeatureRequestMessage = (didFrom: String, mediatorDid: String) =>
    s"""{
      |  "id" : "17f9f122-f762-4ba8-9011-39b9e7efb177",
      |  "type" : "https://didcomm.org/discover-features/2.0/queries",
      |  "to" : [
      |     "$mediatorDid"
      |  ],
      |  "from" : "$didFrom",
      |  "body" : {
      |        "queries": [
      |            { "feature-type": "protocol", "match": ".*routing.*" }
      |        ]
      |    },
      |  "return_route" : "all",
      |  "typ" : "application/didcomm-plain+json"
      |}""".stripMargin
      .fromJson[PlaintextMessage]
  val plaintextDiscoverFeatureRequestMessageNoMatch = (didFrom: String, mediatorDid: String) =>
    s"""{
       |  "id" : "17f9f122-f762-4ba8-9011-39b9e7efb177",
       |  "type" : "https://didcomm.org/discover-features/2.0/queries",
       |  "to" : [
       |     "$mediatorDid"
       |  ],
       |  "from" : "$didFrom",
       |  "body" : {
       |        "queries": [
       |            { "feature-type": "protocol", "match": "routing" }
       |        ]
       |    },
       |  "return_route" : "all",
       |  "typ" : "application/didcomm-plain+json"
       |}""".stripMargin
      .fromJson[PlaintextMessage]
  val plaintextKeyListUpdateRequestMessage = (didFrom: String, mediatorDid: String, recipientDid: String) => s"""{
      |  "id" : "cf64e501-d524-4fd9-8314-4dc4bc652983",
      |  "type" : "https://didcomm.org/coordinate-mediation/2.0/keylist-update",
      |  "to" : [
      |     "$mediatorDid"
      |  ],
      |  "from" : "$didFrom",
      |  "body" : {
      |    "updates" : [
      |      {
      |        "recipient_did" : "$recipientDid",
      |        "action" : "add"
      |      }
      |    ]
      |  },
      |  "return_route" : "all",
      |  "typ" : "application/didcomm-plain+json"
      |}""".stripMargin.fromJson[PlaintextMessage]

  val plaintextKeyListRemoveAliasRequestMessage = (didFrom: String, mediatorDid: String, recipientDid: String) =>
    s"""{
     |  "id" : "cf64e501-d524-4fd9-8314-4dc4bc652983",
     |  "type" : "https://didcomm.org/coordinate-mediation/2.0/keylist-update",
     |  "to" : [
     |    "$mediatorDid"
     |  ],
     |  "from" : "$didFrom",
     |  "body" : {
     |    "updates" : [
     |      {
     |        "recipient_did" : "$recipientDid",
     |        "action" : "remove"
     |      }
     |    ]
     |  },
     |  "return_route" : "all",
     |  "typ" : "application/didcomm-plain+json"
     |}""".stripMargin
      .fromJson[PlaintextMessage]

  val plaintextStatusMessage = (didFrom: String, mediatorDid: String) =>
    s"""{
       |  "id" : "99eeeb12-c5bc-4625-83e7-f3cabf388674",
       |  "type" : "https://didcomm.org/messagepickup/3.0/status",
       |  "to" : [
       |    "$mediatorDid"
       |  ],
       |  "from" : "$didFrom",
       |  "thid" : "thid-responding-to-msg-id",
       |  "body" : {
       |  "recipient_did" : "$didFrom",
       |    "message_count" : 5,
       |    "longest_waited_seconds" : 3600,
       |    "newest_received_time" : 1658085169,
       |    "oldest_received_time" : 1658084293,
       |    "total_bytes" : 8096,
       |    "live_delivery" : false
       |  },
       |  "return_route" : "all",
       |  "typ" : "application/didcomm-plain+json"
       |}""".stripMargin
      .fromJson[PlaintextMessage]

  val plaintextStatusRequestMessage = (didFrom: String, mediatorDid: String) => s"""{
       |  "id" : "99eeeb12-c5bc-4625-83e7-f3cabf388674",
       |  "type" : "https://didcomm.org/messagepickup/3.0/status-request",
       |  "to" : [
       |    "$mediatorDid"
       |  ],
       |  "from" : "$didFrom",
       |  "body" : {},
       |  "return_route" : "all",
       |  "typ" : "application/didcomm-plain+json"
       |}""".stripMargin.fromJson[PlaintextMessage]

  val plaintextDeliveryRequestMessage = (didFrom: String, mediatorDid: String, recipientDid: String) => s"""{
       |  "id" : "5d44cc11-d5da-4e19-ba1a-a5279dfea367",
       |  "type" : "https://didcomm.org/messagepickup/3.0/delivery-request",
       |  "to" : [
       |    "$mediatorDid"
       |  ],
       |  "from" : "$didFrom",
       |  "body" : {
       |    "limit" : 5,
       |    "recipient_did" : "$recipientDid"
       |  },
       |  "return_route" : "all",
       |  "typ" : "application/didcomm-plain+json"
       |}""".stripMargin.fromJson[PlaintextMessage]

  val plaintextMessagesReceivedRequestMessage = (didFrom: String, mediatorDid: String, attachmentID: String) => s"""{
       |  "id" : "f86df694-90b3-4c6f-a2b8-7ebb0247d276",
       |  "type" : "https://didcomm.org/messagepickup/3.0/messages-received",
       |  "to" : [
       |    "$mediatorDid"
       |  ],
       |  "from" : "$didFrom",
       |  "thid" : "maybe-thid-if-responding",
       |  "body" : {
       |    "message_id_list" : [
       |      "$attachmentID"
       |    ]
       |  },
       |  "return_route" : "all",
       |  "typ" : "application/didcomm-plain+json"
       |}""".stripMargin.fromJson[PlaintextMessage]

  val plainTextBasicMessage = (didFrom: String, didTo: String) => s"""{
       |  "id" : "e463a417-7661-4764-b60a-21a3e62ad9cf",
       |  "type" : "https://didcomm.org/basicmessage/2.0/message",
       |  "to" : [
       |    "$didTo"
       |  ],
       |  "from" : "$didFrom",
       |  "body" : {
       |    "content" : "Hello Alice!"
       |  },
       |  "typ" : "application/didcomm-plain+json"
       |}""".stripMargin.fromJson[PlaintextMessage]

  val plaintextForwardMessage = (forwardTo: String, mediatorDid: String, attachedMessage: String) => s"""{
      |  "id" : "f2c8b22f-06ee-4913-b82d-0bc772ade407",
      |  "type" : "https://didcomm.org/routing/2.0/forward",
      |  "to" : [
      |    "$mediatorDid"
      |  ],
      |  "body" : {
      |    "next" : "$forwardTo"
      |  },
      |  "attachments" : [
      |    {
      |      "data" : {
      |        "json" : $attachedMessage
      |      }
      |    }
      |  ],
      |  "typ" : "application/didcomm-plain+json"
      |}""".stripMargin.fromJson[PlaintextMessage]

}
