package common

import abilities.ListenHttp
import io.restassured.builder.RequestSpecBuilder
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

object Agents {
    lateinit var Recipient: Actor
        private set

    fun createAgents() {
        SerenityRest.setDefaultRequestSpecification(
            RequestSpecBuilder().noContentType()
                .addHeader("content-type", "application/didcomm-envelope-enc")
                .build()
        )
        Recipient = Actor.named("Recipient")
            .whoCan(CallAnApi.at(Environments.MEDIATOR_URL))
            .whoCan(ListenHttp.at(Environments.RECIPIENT_LISTENER_HOST, Environments.RECIPIENT_LISTENER_PORT))
        Recipient.remember("did", EdgeAgent.createPeerDid(ListenHttp.asListener(Recipient).endpoint()))
    }
}
