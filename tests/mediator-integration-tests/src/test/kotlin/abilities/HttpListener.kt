package abilities

import common.Environments
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.serenitybdd.screenplay.Actor


object HttpListener {

    private var receivedResponse: String? = null

    fun route(application: Application) {
        application.routing {
            post("/") {
                val json = call.receiveText()
                receivedResponse = json
                call.respond(HttpStatusCode.OK, "Data received")
            }
        }
    }

    init {
        embeddedServer(
            Netty,
            port = Environments.RECIPIENT_LISTENER_PORT,
            host = Environments.RECIPIENT_LISTENER_HOST,
            module = {route(this)})
            .start(wait = false)
    }

    fun endpoint(): String {
        return "http://${Environments.RECIPIENT_LISTENER_HOST}:${Environments.RECIPIENT_LISTENER_PORT}"
    }

    fun receivedResponse(): String? {
        return receivedResponse
    }

    override fun toString(): String {
        return "Listen HTTP port at ${Environments.RECIPIENT_LISTENER_HOST}:${Environments.RECIPIENT_LISTENER_PORT}"
    }
}
