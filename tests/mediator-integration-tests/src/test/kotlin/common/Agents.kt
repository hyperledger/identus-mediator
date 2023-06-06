package common

import abilities.HttpListener
import io.iohk.atala.prism.walletsdk.domain.models.DID
import io.restassured.builder.RequestSpecBuilder
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

object Agents {
    lateinit var Recipient: Actor
        private set

    fun createAgents() {
        Recipient = Actor.named("Recipient")
            .whoCan(CallAnApi.at(Environments.MEDIATOR_URL))
    }
}
