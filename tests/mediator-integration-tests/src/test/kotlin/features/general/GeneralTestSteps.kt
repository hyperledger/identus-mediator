package features.general

import common.*
import common.MediatorErrorMessages.WRONG_CONTENT_TYPE
import interactions.SendDidcommMessage
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.Message
import net.serenitybdd.rest.SerenityRest
import org.apache.http.HttpStatus
import org.apache.http.HttpStatus.SC_BAD_REQUEST

class GeneralTestSteps {

    @When("{actor} sends a didcomm message with the wrong content type")
    fun recipientSendsADidcommMessageWithTheWrongContentType(recipient: Actor) {

        val pingMessage = Message(
            piuri = DidcommMessageTypes.PING_REQUEST,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = TestConstants.EMPTY_BODY,
        )

        recipient.attemptsTo(
            SendDidcommMessage(
                pingMessage,
                TestConstants.UNSUPPORTED_CONTENT_TYPE
            )
        )
    }

    @Then("Mediator returns a correct error message to {actor}")
    fun mediatorReturnsACorrectErrorMessage(recipient: Actor) {
        val httpResponse = SerenityRest.lastResponse()
        recipient.attemptsTo(
            Ensure.that(httpResponse.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST),
            Ensure.that(httpResponse.body().asString()).contains(WRONG_CONTENT_TYPE)
        )
    }
}
