package features.mediation_coordination

import abilities.HttpListener
import common.DidcommMessageTypes.MEDIATE_GRANT
import common.DidcommMessageTypes.MEDIATE_KEYLIST_QUERY
import common.DidcommMessageTypes.MEDIATE_KEYLIST_RESPONSE
import common.DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE
import common.DidcommMessageTypes.MEDIATE_REQUEST
import common.EdgeAgent
import common.Environments
import interactions.SendDidcommMessage
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.Message
import net.serenitybdd.screenplay.ensure.that
import io.cucumber.java.en.Given
import io.iohk.atala.prism.walletsdk.domain.models.DID
import models.*
import net.serenitybdd.core.Serenity

class MediationCoordinationSteps {

    @When("{actor} sends a mediate request message to the mediator")
    fun recipientSendsAMediateRequestMessageToTheMediator(recipient: Actor) {

        val mediateRequest = Message(
            piuri = MEDIATE_REQUEST,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = "any",
        )

        recipient.attemptsTo(
            SendDidcommMessage(mediateRequest)
        )
    }

    @Then("Mediator responds to {actor} with mediate grant message")
    fun mediatorRespondsToHimWithMediateGrantMessage(recipient: Actor) {
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        val didcommBody = MediationGrantResponse.fromJsonString(didcommMessage.body)
        Serenity.recordReportData().withTitle("Mediator response").andContents(didcommMessage.body)
        recipient.attemptsTo(
            that(didcommMessage.piuri).isEqualTo(MEDIATE_GRANT),
            that(didcommMessage.from.toString()).isEqualTo(Environments.MEDIATOR_PEER_DID.toString()),
            that(didcommMessage.to.toString()).isEqualTo(EdgeAgent.peerDID.toString()),
            that(didcommBody.routing_did).isEqualTo(Environments.MEDIATOR_PEER_DID.toString())
        )
    }

    @Given("{actor} successfully set up a connection with the mediator")
    fun recipientSuccessfullySetUpAConnectionWithTheMediator(recipient: Actor) {
        recipientSendsAMediateRequestMessageToTheMediator(recipient)
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        recipient.attemptsTo(
            that(didcommMessage.piuri).isEqualTo(MEDIATE_GRANT),
        )
    }

    @When("{actor} sends a keylist update message to the mediator with a new peer did")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorWithANewPeerDid(recipient: Actor) {
        val newPeerDid = EdgeAgent.createPeerDid(HttpListener.endpoint())
        recipient.attemptsTo(
            that(newPeerDid.toString()).isNotEqualTo(EdgeAgent.peerDID.toString())
        )
        recipient.remember("newPeerDid", newPeerDid)

        val keylistUpdateMessage = Message(
            piuri = MEDIATE_KEYLIST_UPDATE,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = arrayOf(
                    MediationKeylistRequestMessage(
                        action = "add",
                        recipient_did = newPeerDid.toString(),
                    )
                )
            ).toJsonString()
        )
        recipient.attemptsTo(
            SendDidcommMessage(keylistUpdateMessage)
        )
    }

    @Then("Mediator responds to {actor} with a correct keylist update add message")
    fun mediatorRespondsToHimWithACorrectKeylistUpdateAddMessage(recipient: Actor) {
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        val didcommBody = MediationKeylistResponse.fromJsonString(didcommMessage.body)
        Serenity.recordReportData().withTitle("Mediator response").andContents(didcommMessage.body)
        recipient.attemptsTo(
            that(didcommMessage.piuri).isEqualTo(MEDIATE_KEYLIST_RESPONSE),
            that(didcommBody.updated.size).isGreaterThan(0),
            that(didcommBody.updated[0].result).isEqualTo("success"),
            that(didcommBody.updated[0].action).isEqualTo("add"),
            that(didcommBody.updated[0].routing_did).isEqualTo(recipient.recall<DID>("newPeerDid").toString()),
        )
    }

    @When("{actor} sends a keylist query message to the mediator")
    fun recipientSendsAKeylistQueryMessageToTheMediator(recipient: Actor) {
        val keylistUpdateMessage = Message(
            piuri = MEDIATE_KEYLIST_QUERY,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistQueryRequest(
                paginate = Paginate(
                    limit = 2,
                    offset = 0
                )
            ).toJsonString()
        )
        recipient.attemptsTo(
            SendDidcommMessage(keylistUpdateMessage)
        )
    }

    @Then("Mediator responds to {actor} with keylist message containing the current list of keys")
    fun mediatorRespondsToRecipientWithKeylistMessageContainingTheCurrentListOfKeys(recipient: Actor) {
        // TODO: NOT IMPLEMENTED
    }

    @When("{actor} sends a keylist update message to the mediator to remove the last alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveAKey(recipient: Actor) {
        val keylistUpdateMessage = Message(
            piuri = MEDIATE_KEYLIST_UPDATE,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = arrayOf(
                    MediationKeylistRequestMessage(
                        action = "remove",
                        recipient_did = EdgeAgent.peerDID.toString()
                    )
                )
            ).toJsonString()
        )
        recipient.attemptsTo(
            SendDidcommMessage(keylistUpdateMessage)
        )
    }

    @When("{actor} sends a keylist update message to the mediator to remove added alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveAddedAlias(recipient: Actor) {
        val keylistUpdateMessage = Message(
            piuri = MEDIATE_KEYLIST_UPDATE,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = arrayOf(
                    MediationKeylistRequestMessage(
                        action = "remove",
                        recipient_did = recipient.recall<DID>("newPeerDid").toString()
                    )
                )
            ).toJsonString()
        )
        recipient.attemptsTo(
            SendDidcommMessage(keylistUpdateMessage)
        )
    }

    @Then("Mediator responds to {actor} with a correct keylist update remove message")
    fun mediatorRespondsToRecipientWithACorrectKeylistUpdateRemoveMessage(recipient: Actor) {
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        val didcommBody = MediationKeylistResponse.fromJsonString(didcommMessage.body)
        Serenity.recordReportData().withTitle("Mediator response").andContents(didcommMessage.body)
        recipient.attemptsTo(
            that(didcommMessage.piuri).isEqualTo(MEDIATE_KEYLIST_RESPONSE),
            that(didcommBody.updated.size).isGreaterThan(0),
            that(didcommBody.updated[0].result).isEqualTo("success"),
            that(didcommBody.updated[0].action).isEqualTo("remove"),
        )
    }

    @When("{actor} sends a keylist update message to the mediator to remove not existing alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveNotExistingAlias(recipient: Actor) {
        val keylistUpdateMessage = Message(
            piuri = MEDIATE_KEYLIST_UPDATE,
            from = EdgeAgent.peerDID,
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = arrayOf(
                    MediationKeylistRequestMessage(
                        action = "remove",
                        recipient_did = "did:example:123456789abcdefghi"
                    )
                )
            ).toJsonString()
        )
        recipient.attemptsTo(
            SendDidcommMessage(keylistUpdateMessage)
        )
    }

    @Then("Mediator responds to {actor} with a message with no_change status")
    fun mediatorRespondsToRecipientWithAMessageWithNo_changeStatus(recipient: Actor) {
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        val didcommBody = MediationKeylistResponse.fromJsonString(didcommMessage.body)
        Serenity.recordReportData().withTitle("Mediator response").andContents(didcommMessage.body)
        recipient.attemptsTo(
            that(didcommMessage.piuri).isEqualTo(MEDIATE_KEYLIST_RESPONSE),
            that(didcommBody.updated.size).isGreaterThan(0),
            that(didcommBody.updated[0].result).isEqualTo("no_change"),
            that(didcommBody.updated[0].action).isEqualTo("remove"),
        )
    }
}
