package models

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
    val longest_waited_seconds: Int = 0,
    val newest_received_time: Int = 0,
    val oldest_received_time: Int = 0,
    val total_bytes: Int = 0,
    val live_delivery: Boolean = false
): JsonEncoded
