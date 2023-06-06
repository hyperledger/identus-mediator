package features.mediation_coordination

import abilities.ListenHttp
import common.DidcommMessageTypes
import common.EdgeAgent
import common.Environments
import interactions.SendDidcommMessage
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.DID
import io.iohk.atala.prism.walletsdk.domain.models.Message
import io.iohk.atala.prism.walletsdk.prismagent.protocols.ProtocolType
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.ensure.that
import org.apache.http.HttpStatus

class MediationCoordinationSteps {

    @When("{actor} sends a mediate request message to the mediator")
    fun recipientSendsAMediateRequestMessageToTheMediator(recipient: Actor) {

        println(Environments.MEDIATOR_PEER_DID)

        println(recipient.recall<DID>("did").toString())

        val pingMessage = Message(
            piuri = "https://didcomm.org/coordinate-mediation/2.0/mediate-request",
            from = recipient.recall<DID>("did"),
            to = Environments.MEDIATOR_PEER_DID,
            body = "123",
        )

        recipient.attemptsTo(
            SendDidcommMessage(pingMessage)
        )
    }

    @Then("Mediator responds to {actor} with mediate grant message")
    fun mediatorRespondsToHimWithMediateGrantMessage(recipient: Actor) {
        val httpResponse = SerenityRest.lastResponse().body().asString()
        println(SerenityRest.lastResponse().body().asString())
        println(SerenityRest.lastResponse().statusCode().toString())
//        val response = EdgeAgent.unpackMessage(httpResponse.body.toString())
//        println(response)
    }
}