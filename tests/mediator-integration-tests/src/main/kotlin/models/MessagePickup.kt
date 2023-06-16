package models

//{
//    "id": "123456780",
//    "type": "https://didcomm.org/messagepickup/3.0/status",
//    "body": {
//    "recipient_did": "<did for messages>",
//    "message_count": 7,
//    "longest_waited_seconds": 3600,
//    "newest_received_time": 1658085169,
//    "oldest_received_time": 1658084293,
//    "total_bytes": 8096,
//    "live_delivery": false
//}

import kotlinx.serialization.Serializable

@Serializable
data class MessagePickupStatus(
    val id: String,
    val type: String,
    val body: MessagePickupStatusBody
): JsonEncoded

@Serializable
data class MessagePickupStatusBody(
    val recipient_did: String,
    val message_count: Int,
    val longest_waited_seconds: Int,
    val newest_received_time: Int,
    val oldest_received_time: Int,
    val total_bytes: Int,
    val live_delivery: Boolean
): JsonEncoded
