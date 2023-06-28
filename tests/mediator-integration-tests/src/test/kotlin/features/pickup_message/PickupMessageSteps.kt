package features.pickup_message

import common.*
import interactions.SendDidcommMessage
import io.cucumber.java.en.Given
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.AttachmentBase64
import io.iohk.atala.prism.walletsdk.domain.models.DID
import java.util.Base64
import io.iohk.atala.prism.walletsdk.domain.models.Message
import io.iohk.atala.prism.walletsdk.mercury.forward.ForwardMessage

class PickupMessageSteps {

    @Given("{actor} sent a forward message to {actor}")
    fun senderSentAForwardMessageToRecipient(sender: Actor, recipient: Actor) {

        val message = Message(
            piuri = DidcommMessageTypes.BASIC_MESSAGE,
            from = sender.recall<DID>("peerDid"),
            to = recipient.recall<DID>("peerDid"),
            body = TestConstants.CONST_BODY,
        )
        sender.remember("initialMessage", message)

        val forwardMessage = ForwardMessage(
            body = ForwardMessage.ForwardBody(message.to.toString()),
            encryptedMessage = EdgeAgent.packMessage(message),
            from = message.from!!,
            to = Environments.MEDIATOR_PEER_DID,
        ).makeMessage()

        recipient.attemptsTo(
            SendDidcommMessage(forwardMessage)
        )

    }

    @When("{actor} sends a status-request message")
    fun recipientSendsAStatusRequestMessage(recipient: Actor) {
        recipient.attemptsTo(
            SendDidcommMessage(
                Message(
                    piuri = DidcommMessageTypes.PICKUP_STATUS_REQUEST,
                    from = recipient.recall<DID>("peerDid"),
                    to = Environments.MEDIATOR_PEER_DID,
                    body = """ {"recipient_did": "${recipient.recall<DID>("peerDid")}"} """,
                )
            )
        )
    }

    @Then("Mediator responds with a status message detailing the queued messages of {actor}")
    fun mediatorRespondsWithAStatusMessageDetailingTheQueuedMessagesOfRecipient(recipient: Actor) {
        TODO("Not yet implemented https://input-output.atlassian.net/browse/ATL-4883")
    }

    @When("{actor} sends a delivery-request message")
    fun recipientSendsADeliveryRequestMessage(recipient: Actor) {
        recipient.attemptsTo(
            SendDidcommMessage(
                Message(
                    piuri = DidcommMessageTypes.PICKUP_DELIVERY_REQUEST,
                    from = recipient.recall<DID>("peerDid"),
                    to = Environments.MEDIATOR_PEER_DID,
                    body = """ { "recipient_did": "${recipient.recall<DID>("peerDid")}", "limit": 3 } """,
                )
            )
        )
    }

    @Then("Mediator delivers message of {actor} to {actor}")
    fun mediatorDeliversMessageOfSenderToRecipient(sender: Actor, recipient: Actor) {
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        val data = didcommMessage.attachments.first().data as AttachmentBase64
        val decoded = Base64.getUrlDecoder().decode(data.base64).decodeToString()
        val achievedMessage = EdgeAgent.unpackMessage(decoded)
        val initialMessage = sender.recall<Message>("initialMessage")

        recipient.attemptsTo(
            Ensure.that(didcommMessage.attachments.size).isEqualTo(1),
            Ensure.that(achievedMessage.id).isEqualTo(initialMessage.id),
            Ensure.that(achievedMessage.body).isEqualTo(initialMessage.body),
            Ensure.that(achievedMessage.from.toString()).isEqualTo(initialMessage.from.toString()),
            Ensure.that(achievedMessage.to.toString()).isEqualTo(initialMessage.to.toString())
        )
    }
}