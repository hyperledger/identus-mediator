package models

import kotlinx.serialization.Serializable

@Serializable
data class MediationGrantResponse(
    val routing_did: String
): JsonEncoded

@Serializable
data class MediationKeylistRequest(
    val updates: List<MediationKeylistRequestMessage>
): JsonEncoded

@Serializable
data class MediationKeylistUpdateResponse(
    val updated: List<MediationKeylistResponseMessage>
): JsonEncoded

@Serializable
data class MediationKeylistResponse(
    val keys: List<MediationKeylistKey>
): JsonEncoded

@Serializable
data class MediationKeylistKey(
    val recipient_did: String
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
    val recipient_did: String
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
