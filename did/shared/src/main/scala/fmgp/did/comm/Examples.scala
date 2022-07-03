package fmgp.did.comm

object Examples {

  /** A DIDComm message in its plaintext form, not packaged into any protective envelope, is known as a DIDComm
    * plaintext message.
    */
  val dcpmLinkSVG = "https://identity.foundation/didcomm-messaging/collateral/dcpm.svg"

  /** A DIDComm signed message is a signed JWM envelope that associates a non-repudiable signature with the plaintext
    * message inside it.
    */
  val dcsmLinkSVG = "https://identity.foundation/didcomm-messaging/collateral/dcsm.svg"

  /** A DIDComm encrypted message is an encrypted JWM. It hides its content from all but authorized recipients,
    * discloses and proves the sender to exactly and only those recipients, and provides integrity guarantees. It is
    * important in privacy-preserving routing. It is what normally moves over network transports in DIDComm Messaging
    * applications, and is the safest format for storing DIDComm Messaging data at rest.
    */
  val dcemLinkSVG = "https://identity.foundation/didcomm-messaging/collateral/dcem.svg"

  val plaintextMessageExample = """
  {
  "id": "1234567890",
  "type": "<message-type-uri>",
  "from": "did:example:alice",
  "to": ["did:example:bob"],
  "created_time": 1516269022,
  "expires_time": 1516385931,
  "body": {
    "message_type_specific_attribute": "and its value",
    "another_attribute": "and its value"
  }
}
""".stripMargin

  def attachmentExample = """
{
  "type": "<sometype>",
  "to": [
    "did:example:mediator"
  ],
  "body": {
    "attachment_id": "1",
    "encrypted_details": {
      "id": "x",
      "encrypted_to": "",
      "other_details": "about attachment"
    }
  },
  "attachments": [
    {
      "id": "1",
      "description": "example b64 encoded attachment",
      "data": {
        "base64": "WW91ciBob3ZlcmNyYWZ0IGlzIGZ1bGwgb2YgZWVscw=="
      }
    },
    {
      "id": "2",
      "description": "example linked attachment",
      "data": {
        "hash": "<multi-hash>",
        "links": [
          "https://path/to/resource"
        ]
      }
    },
    {
      "id": "x",
      "description": "example encrypted DIDComm message as attachment",
      "media_type": "application/didcomm-encrypted+json",
      "data": {
        "json": {
          //jwe json structure
        }
      }
    }
  ]
}
""".stripMargin
}
