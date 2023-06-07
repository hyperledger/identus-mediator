package models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class MediationGrantResponse(
    val routing_did: String = "",
) {
    companion object {
        fun fromJsonString(json: String): MediationGrantResponse {
            return Json.decodeFromString<MediationGrantResponse>(json)
        }
    }
}

@Serializable
data class MediationKeylistRequest(
    val updates: Array<MediationKeylistRequestMessage> = arrayOf()
) {
    fun toJsonString(): String {
        return Json.encodeToString(this)
    }
}

@Serializable
data class MediationKeylistResponse(
    val updated: Array<MediationKeylistResponseMessage> = arrayOf(),
) {
    companion object {
        fun fromJsonString(json: String): MediationKeylistResponse {
            return Json.decodeFromString<MediationKeylistResponse>(json)
        }
    }
}

@Serializable
data class MediationKeylistRequestMessage(
    val action: String = "",
    val recipient_did: String = "",
)

@Serializable
data class MediationKeylistResponseMessage(
    val result: String = "",
    val action: String = "",
    val routing_did: String = "",
)

//"paginate": {
//    "limit": 30,
//    "offset": 0
//}

@Serializable
data class MediationKeylistQueryRequest(
    val paginate: Paginate = Paginate(),
) {
    fun toJsonString(): String {
        return Json.encodeToString(this)
    }
}

@Serializable
data class Paginate(
    val limit: Int = 0,
    val offset: Int = 0,
)
