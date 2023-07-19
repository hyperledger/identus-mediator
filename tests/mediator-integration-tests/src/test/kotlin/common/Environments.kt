package common

import io.iohk.atala.prism.walletsdk.domain.models.DID

object Environments {
    val MEDIATOR_PEER_DID = DID(System.getenv("MEDIATOR_PEER_DID") ?: "did:peer:2.Ez6LSfKLqi2VZj2p84qE2AnHCv4YQtJoZXG7SmxoFGiqLuqa8.Vz6Mkf92NMfWKgLWYpGUfmsGiuEQfQSoLCuKDmFAhxQgkCUFY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ")
    val MEDIATOR_URL = System.getenv("MEDIATOR_URL") ?: "http://localhost:8080"
    val RECIPIENT_LISTENER_HOST = System.getenv("RECIPIENT_LISTENER_URL") ?: "0.0.0.0"
    val RECIPIENT_LISTENER_PORT = (System.getenv("RECIPIENT_LISTENER_PORT") ?: "9999").toInt()
}
