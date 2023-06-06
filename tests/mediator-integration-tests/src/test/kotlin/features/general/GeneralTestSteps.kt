package features.general

import common.EdgeAgent
import common.Environments
import common.MediatorErrorMessages.WRONG_CONTENT_TYPE
import interactions.SendDidcommMessage
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.DID
import io.iohk.atala.prism.walletsdk.domain.models.Message
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.ensure.that
import org.apache.http.HttpStatus.SC_BAD_REQUEST

class GeneralTestSteps {

    @When("{actor} sends a didcomm message with the wrong content type")
    fun recipientSendsADidcommMessageWithTheWrongContentType(recipient: Actor) {
        val pingMessage = Message(
            piuri = "any",
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = "any",
        )
        recipient.attemptsTo(
            SendDidcommMessage(pingMessage, "unsupported-type+json")
        )
    }

    @Then("Mediator returns a correct error message to {actor}")
    fun mediatorReturnsACorrectErrorMessage(recipient: Actor) {
        val httpResponse = SerenityRest.lastResponse()
        recipient.attemptsTo(
            that(httpResponse.statusCode()).isEqualTo(SC_BAD_REQUEST),
            that(httpResponse.body().asString()).contains(WRONG_CONTENT_TYPE)
        )
    }
}
