package common

object DidcommMessageTypes {
    const val PING_REQUEST = "https://didcomm.org/trust-ping/2.0/ping"
    const val PING_RESPONSE = "https://didcomm.org/trust-ping/2.0/ping-response"
    const val MEDIATE_REQUEST = "https://didcomm.org/coordinate-mediation/2.0/mediate-request"
    const val MEDIATE_GRANT = "https://didcomm.org/coordinate-mediation/2.0/mediate-grant"
    const val MEDIATE_KEYLIST_UPDATE = "https://didcomm.org/coordinate-mediation/2.0/keylist-update"
    const val MEDIATE_KEYLIST_RESPONSE = "https://didcomm.org/coordinate-mediation/2.0/keylist-update-response"
    const val MEDIATE_KEYLIST_QUERY = "https://didcomm.org/coordinate-mediation/2.0/keylist-query"
}
