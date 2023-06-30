package common

object TestConstants {
    const val EMPTY_BODY: String = "{}"
    const val CONST_BODY: String = """{"key":"value"}"""
    const val UNSUPPORTED_CONTENT_TYPE: String = "unsupported-type+json"
    const val DIDCOMM_V2_CONTENT_TYPE_ENCRYPTED: String = "application/didcomm-encrypted+json"
    const val DIDCOMM_V2_CONTENT_TYPE_PLAIN: String = "application/didcomm-plain+json"
    const val MEDIATOR_COORDINATION_ACTION_ADD: String = "add"
    const val MEDIATOR_COORDINATION_ACTION_REMOVE: String = "remove"
    const val MEDIATOR_COORDINATION_ACTION_RESULT_SUCCESS: String = "success"
    const val MEDIATOR_COORDINATION_ACTION_RESULT_NO_CHANGE: String = "no_change"
    const val EXAMPLE_DID: String = "did:example:123456789abcdefghi"
}
