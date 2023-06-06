package abilities

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.serenitybdd.screenplay.Ability
import net.serenitybdd.screenplay.Actor


class ListenHttp private constructor(val host: String, val port: Int) : Ability {

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
            port = port,
            host = host,
            module = {route(this)})
            .start(wait = false)
    }

    fun endpoint(): String {
        return "http://$host:$port"
    }

    fun receivedResponse(): String? {
        return receivedResponse
    }

    override fun toString(): String {
        return "Listen HTTP port at $host:$port"
    }

    companion object {
        fun at(host: String, port: Int): ListenHttp {
            return ListenHttp(host, port)
        }

        fun asListener(actor: Actor): ListenHttp {
            return actor.abilityTo(ListenHttp::class.java)
        }
    }
}
