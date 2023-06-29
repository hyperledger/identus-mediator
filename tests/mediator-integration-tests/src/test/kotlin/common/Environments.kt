package common

import io.iohk.atala.prism.walletsdk.domain.models.DID

object Environments {
    val MEDIATOR_PEER_DID = DID(System.getenv("MEDIATOR_PEER_DID") ?: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9rOHMtaW50LmF0YWxhcHJpc20uaW8vbWVkaWF0b3IiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19")
    val MEDIATOR_URL = System.getenv("MEDIATOR_URL") ?: "http://localhost:8080"
    val RECIPIENT_LISTENER_HOST = System.getenv("RECIPIENT_LISTENER_URL") ?: "0.0.0.0"
    val RECIPIENT_LISTENER_PORT = (System.getenv("RECIPIENT_LISTENER_PORT") ?: "9999").toInt()
}
