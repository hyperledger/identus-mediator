package features.mediation_coordination

import common.DidcommMessageTypes.MEDIATE_GRANT
import common.DidcommMessageTypes.MEDIATE_REQUEST
import common.EdgeAgent
import common.Environments
import interactions.SendDidcommMessage
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.Message
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.ensure.that
import org.apache.http.HttpStatus.SC_OK

class MediationCoordinationSteps {

    @When("{actor} sends a mediate request message to the mediator")
    fun recipientSendsAMediateRequestMessageToTheMediator(recipient: Actor) {

        val pingMessage = Message(
            piuri = MEDIATE_REQUEST,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = "any",
        )

        recipient.attemptsTo(
            SendDidcommMessage(pingMessage)
        )
    }

    @Then("Mediator responds to {actor} with mediate grant message")
    fun mediatorRespondsToHimWithMediateGrantMessage(recipient: Actor) {
        val httpResponse = SerenityRest.lastResponse()
        val didcommMessage = EdgeAgent.unpackMessage(httpResponse.asString())

        recipient.attemptsTo(
            that(httpResponse.statusCode).isEqualTo(SC_OK),
            that(didcommMessage.piuri).isEqualTo(MEDIATE_GRANT),
            that(didcommMessage.from.toString()).isEqualTo(Environments.MEDIATOR_PEER_DID.toString()),
            that(didcommMessage.to.toString()).isEqualTo(EdgeAgent.peerDID.toString()),
            that(didcommMessage.body).contains("routing_did"),
            that(didcommMessage.body).contains(Environments.MEDIATOR_PEER_DID.toString()),
        )

    }
}
