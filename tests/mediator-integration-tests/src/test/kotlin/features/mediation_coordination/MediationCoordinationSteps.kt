package features.mediation_coordination

import abilities.HttpListener
import common.*
import interactions.SendDidcommMessage
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.Message
import io.cucumber.java.en.Given
import io.iohk.atala.prism.walletsdk.domain.models.DID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import models.*

class MediationCoordinationSteps {

    @When("{actor} sends a mediate request message to the mediator")
    fun recipientSendsAMediateRequestMessageToTheMediator(recipient: Actor) {

        val mediateRequest = Message(
            piuri = DidcommMessageTypes.MEDIATE_REQUEST,
            from = recipient.recall<DID>("peerDid"),
            to = Environments.MEDIATOR_PEER_DID,
            body = TestConstants.EMPTY_BODY,
        )

        recipient.attemptsTo(
            SendDidcommMessage(mediateRequest)
        )
    }

    @Then("Mediator responds to {actor} with mediate grant message")
    fun mediatorRespondsToHimWithMediateGrantMessage(recipient: Actor) {
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        val didcommBody = Json.decodeFromString<MediationGrantResponse>(didcommMessage.body)
        recipient.attemptsTo(
            Ensure.that(didcommMessage.piuri).isEqualTo(DidcommMessageTypes.MEDIATE_GRANT),
            Ensure.that(didcommMessage.from.toString()).isEqualTo(Environments.MEDIATOR_PEER_DID.toString()),
            Ensure.that(didcommMessage.to.toString()).isEqualTo(recipient.recall<DID>("peerDid").toString()),
            Ensure.that(didcommBody.routing_did).isEqualTo(Environments.MEDIATOR_PEER_DID.toString())
        )
    }

    @Given("{actor} successfully set up a connection with the mediator")
    fun recipientSuccessfullySetUpAConnectionWithTheMediator(recipient: Actor) {
        recipientSendsAMediateRequestMessageToTheMediator(recipient)
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        recipient.attemptsTo(
            Ensure.that(didcommMessage.piuri).isEqualTo(DidcommMessageTypes.MEDIATE_GRANT),
        )
    }

    @When("{actor} sends a keylist update message to the mediator with a new peer did")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorWithANewPeerDid(recipient: Actor) {
        val newPeerDid = EdgeAgent.createPeerDid(HttpListener.endpoint())
        recipient.attemptsTo(
            Ensure.that(newPeerDid.toString()).isNotEqualTo(recipient.recall<DID>("peerDid").toString())
        )
        recipient.remember("newPeerDid", newPeerDid)

        val keylistUpdateMessage = Message(
            piuri = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE,
            from = recipient.recall<DID>("peerDid"),
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = listOf(
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
        val didcommBody = Json.decodeFromString<MediationKeylistUpdateResponse>(didcommMessage.body)
        recipient.attemptsTo(
            Ensure.that(didcommMessage.piuri).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE_RESPONSE),
            Ensure.that(didcommBody.updated.size).isGreaterThan(0),
            Ensure.that(didcommBody.updated[0].result).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_RESULT_SUCCESS),
            Ensure.that(didcommBody.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_ADD),
            Ensure.that(didcommBody.updated[0].recipient_did).isEqualTo(
                recipient.recall<DID>("newPeerDid").toString()
            ),
        )
    }

    @When("{actor} sends a keylist query message to the mediator")
    fun recipientSendsAKeylistQueryMessageToTheMediator(recipient: Actor) {
        val keyListQueryMessage = Message(
            piuri = DidcommMessageTypes.MEDIATE_KEYLIST_QUERY,
            from = recipient.recall<DID>("peerDid"),
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistQueryRequest(
                paginate = Paginate(
                    limit = 2,
                    offset = 0
                )
            ).toJsonString()
        )
        recipient.attemptsTo(
            SendDidcommMessage(keyListQueryMessage)
        )
    }

    @Then("Mediator responds to {actor} with keylist message containing the current list of keys")
    fun mediatorRespondsToRecipientWithKeylistMessageContainingTheCurrentListOfKeys(recipient: Actor) {
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        val didcommBody = Json.decodeFromString<MediationKeylistResponse>(didcommMessage.body)
        recipient.attemptsTo(
            Ensure.that(didcommMessage.piuri).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST),
            Ensure.that(didcommBody.keys.size).isGreaterThan(0),
            Ensure.that(didcommBody.keys.last().recipient_did).isEqualTo(recipient.recall<DID>("peerDid").toString())
        )
    }

    @When("{actor} sends a keylist update message to the mediator to remove the last alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveAKey(recipient: Actor) {
        val keylistUpdateMessage = Message(
            piuri = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE,
            from = recipient.recall<DID>("peerDid"),
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = listOf(
                    MediationKeylistRequestMessage(
                        action = "remove",
                        recipient_did = recipient.recall<DID>("peerDid").toString()
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
            piuri = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE,
            from = recipient.recall<DID>("peerDid"),
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = listOf(
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
        val didcommBody = Json.decodeFromString<MediationKeylistUpdateResponse>(didcommMessage.body)
        recipient.attemptsTo(
            Ensure.that(didcommMessage.piuri).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE_RESPONSE),
            Ensure.that(didcommBody.updated.size).isGreaterThan(0),
            Ensure.that(didcommBody.updated[0].result).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_RESULT_SUCCESS),
            Ensure.that(didcommBody.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE),
        )
    }

    @When("{actor} sends a keylist update message to the mediator to remove not existing alias")
    fun recipientSendsAKeylistUpdateMessageToTheMediatorToRemoveNotExistingAlias(recipient: Actor) {

        val keylistUpdateMessage = Message(
            piuri = DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE,
            from = recipient.recall<DID>("peerDid"),
            to = Environments.MEDIATOR_PEER_DID,
            body = MediationKeylistRequest(
                updates = listOf(
                    MediationKeylistRequestMessage(
                        action = TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE,
                        recipient_did = TestConstants.EXAMPLE_DID
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
        val didcommBody = Json.decodeFromString<MediationKeylistUpdateResponse>(didcommMessage.body)
        recipient.attemptsTo(
            Ensure.that(didcommMessage.piuri).isEqualTo(DidcommMessageTypes.MEDIATE_KEYLIST_UPDATE_RESPONSE),
            Ensure.that(didcommBody.updated.size).isGreaterThan(0),
            Ensure.that(didcommBody.updated[0].result).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_RESULT_NO_CHANGE),
            Ensure.that(didcommBody.updated[0].action).isEqualTo(TestConstants.MEDIATOR_COORDINATION_ACTION_REMOVE),
        )
    }
}
