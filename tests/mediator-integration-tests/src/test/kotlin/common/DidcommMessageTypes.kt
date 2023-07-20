package common

object DidcommMessageTypes {
    const val PING_REQUEST = "https://didcomm.org/trust-ping/2.0/ping"
    const val PING_RESPONSE = "https://didcomm.org/trust-ping/2.0/ping-response"
    const val MEDIATE_REQUEST = "https://didcomm.org/coordinate-mediation/2.0/mediate-request"
    const val MEDIATE_GRANT = "https://didcomm.org/coordinate-mediation/2.0/mediate-grant"
    const val MEDIATE_KEYLIST_UPDATE = "https://didcomm.org/coordinate-mediation/2.0/keylist-update"
    const val MEDIATE_KEYLIST_UPDATE_RESPONSE = "https://didcomm.org/coordinate-mediation/2.0/keylist-update-response"
    const val MEDIATE_KEYLIST = "https://didcomm.org/coordinate-mediation/2.0/keylist"
    const val MEDIATE_KEYLIST_QUERY = "https://didcomm.org/coordinate-mediation/2.0/keylist-query"
    const val FORWARD_REQUEST = "https://didcomm.org/routing/2.0/forward"
    const val BASIC_MESSAGE = "https://didcomm.org/basicmessage/2.0/message"
    const val PICKUP_STATUS_REQUEST = "https://didcomm.org/messagepickup/3.0/status-request"
    const val PICKUP_DELIVERY_REQUEST = "https://didcomm.org/messagepickup/3.0/delivery-request"
    const val PICKUP_STATUS = "https://didcomm.org/messagepickup/3.0/status"
    const val CONNECTION_RESPONSE = "https://atalaprism.io/mercury/connections/1.0/response"
}
