package models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed interface JsonEncoded {
    fun toJsonString(): String {
        return Json.encodeToString(this)
    }
}

@Serializable
data class MediationGrantResponse(
    val routing_did: String
): JsonEncoded

@Serializable
data class MediationKeylistRequest(
    val updates: Array<MediationKeylistRequestMessage>
): JsonEncoded

@Serializable
data class MediationKeylistResponse(
    val updated: Array<MediationKeylistResponseMessage>
): JsonEncoded

@Serializable
data class MediationKeylistRequestMessage(
    val action: String,
    val recipient_did: String
): JsonEncoded

@Serializable
data class MediationKeylistResponseMessage(
    val result: String,
    val action: String,
    val routing_did: String
): JsonEncoded

@Serializable
data class MediationKeylistQueryRequest(
    val paginate: Paginate
): JsonEncoded

@Serializable
data class Paginate(
    val limit: Int,
    val offset: Int
): JsonEncoded
