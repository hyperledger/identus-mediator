package features.pickup_message

import abilities.HttpListener
import common.*
import common.TestConstants.DIDCOMM_V2_CONTENT_TYPE_PLAIN
import interactions.SendDidcommMessage
import io.cucumber.java.en.Given
import net.serenitybdd.screenplay.Actor
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.iohk.atala.prism.walletsdk.domain.models.AttachmentBase64
import io.iohk.atala.prism.walletsdk.domain.models.DID
import io.iohk.atala.prism.walletsdk.mercury.forward.ForwardMessage
import io.iohk.atala.prism.walletsdk.prismagent.protocols.pickup.PickupRunner
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.EncoderConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import models.MessagePickupStatus
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.rest.interactions.Post
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.Attachment
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.model.PackEncryptedParams
import org.didcommx.didcomm.model.UnpackParams
import org.didcommx.didcomm.utils.fromJsonToMap
import java.util.Base64
import io.iohk.atala.prism.walletsdk.domain.models.Message as Message1

class PickupMessageSteps {

    @Given("{actor} shailesh step")
    fun shailesh_send(recipient: Actor) {

        val bob = AgentPeerService.makeAgent(serviceEndpoint = "http://localhost:9999/")
        println(bob.did.value)
        println(bob.didDocument)

        val alice = AgentPeerService.makeAgent(serviceEndpoint = "http://localhost:9999/")
        println(alice.did.value)
        println(alice.didDocument)

        val mediatorDID = Environments.MEDIATOR_PEER_DID

        val mediator = AgentPeerService.makeAgentFromPeerDid(mediatorDID.toString())

        val didComm = DIDComm(mediator.getDidDocResolverInMemory(), alice.getSecretResolverInMemory())


        val messageMediationRequest = Message.builder(
            id = "1234567890",
            body = mapOf(),
            type = "https://didcomm.org/coordinate-mediation/2.0/mediate-request"
        ).from(alice.did.value)
            .to(listOf(mediator.did.value))
            .build()


        val packMediation = didComm.packEncrypted(
            PackEncryptedParams.builder(messageMediationRequest, mediator.did.value)
                .from(alice.did.value)
                .build()
        )

        val specMediation = RequestSpecBuilder().noContentType()
            .setContentType(TestConstants.DIDCOMM_V2_CONTENT_TYPE_ENCRYPTED)
            .setConfig(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig
                            .encoderConfig()
                            .appendDefaultContentCharsetToContentTypeIfUndefined(false))
            )
            .setBody(packMediation.packedMessage)
            .build()

        recipient.attemptsTo(
            Post.to("/").with {
                it.spec(specMediation)
            }
        )

        val basicMessage = Message.builder(
            id = "12345678",
            body = mapOf(),
            type = DidcommMessageTypes.BASIC_MESSAGE
        ).from(alice.did.value)
            .to(listOf(alice.did.value))
            .build()

        //Encrypt Message
        val packResult = didComm.packEncrypted(
            PackEncryptedParams.builder(basicMessage, alice.did.value)
                .from(alice.did.value)
                .build()
        )

        println(packResult.packedMessage)

        val forwardMessage = Message.builder(
            id = "1234567890",
            body = mapOf("next" to alice.did.value),
            type = "https://didcomm.org/routing/2.0/forward",
        ).from(alice.did.value)
            .to(listOf(mediator.did.value))
            .attachments(
                listOf(
                    Attachment.builder(
                        id = "1234560",
                        data = Attachment.Data.Json(fromJsonToMap(packResult.packedMessage)),
                    ).build()
                )
            )
            .build()

        //Encrypt Message
        val packResultForward = didComm.packEncrypted(
            PackEncryptedParams.builder(forwardMessage, mediatorDID.toString())
                .from(alice.did.value)
                .build()
        )
        println("*************************************************************************************")
        println(packResultForward.packedMessage)
        println("*************************************************************************************")

        val spec = RequestSpecBuilder().noContentType()
            .setContentType(TestConstants.DIDCOMM_V2_CONTENT_TYPE_ENCRYPTED)
            .setConfig(
                RestAssured.config()
                .encoderConfig(
                    EncoderConfig
                        .encoderConfig()
                        .appendDefaultContentCharsetToContentTypeIfUndefined(false))
            )
            .setBody(packResultForward.packedMessage)
            .build()

        recipient.attemptsTo(
            Post.to("/").with {
                it.spec(spec)
            }
        )

        val deliveryRequest = Message.builder(
            id = "12345",
            body = mapOf("recipient_did" to alice.did.value, "limit" to 3),
            type = DidcommMessageTypes.PICKUP_DELIVERY_REQUEST
        ).from(alice.did.value)
            .to(listOf(mediator.did.value))
            .build()

        val packResultDeliveryRequest = didComm.packEncrypted(
            PackEncryptedParams.builder(deliveryRequest, mediator.did.value)
                .from(alice.did.value)
                .build()
        )

        val specDeliveryRequest = RequestSpecBuilder().noContentType()
            .setContentType(TestConstants.DIDCOMM_V2_CONTENT_TYPE_ENCRYPTED)
            .setConfig(
                RestAssured.config()
                    .encoderConfig(
                        EncoderConfig
                            .encoderConfig()
                            .appendDefaultContentCharsetToContentTypeIfUndefined(false))
            )
            .setBody(packResultDeliveryRequest.packedMessage)
            .build()

        recipient.attemptsTo(
            Post.to("/").with {
                it.spec(specDeliveryRequest)
            }
        )

        val body = SerenityRest.lastResponse().body.asString()
        val messageUnpacked =  body?.let {didComm.unpack(UnpackParams.Builder(body).build())}
        println("Decrypted" +  messageUnpacked?.message)

        messageUnpacked?.message!!.attachments!!.forEach {
            val attachment = it.data as Attachment.Data.Base64


            println(attachment.base64)

            val decoded = Base64.getUrlDecoder().decode(attachment.base64).decodeToString()
            println("attachment: $decoded")

            // DECRYPT ERROR IS HAPPENING HERE
            val unpacked = didComm.unpack(UnpackParams.Builder(decoded).build())
        }


    }

    @Given("{actor} sent a forward message to {actor}")
    fun senderSentAForwardMessageToRecipient(sender: Actor, recipient: Actor) {

        val message = Message1(
            piuri = DidcommMessageTypes.BASIC_MESSAGE,
            from = recipient.recall<DID>("peerDid"),
            to = recipient.recall<DID>("peerDid"),
            body = TestConstants.CONST_BODY,
        )

        println(recipient.recall<DID>("peerDid"))

//        val message = Message(
//            piuri = DidcommMessageTypes.PING_REQUEST,
//            from = recipient.recall<DID>("peerDid"),
//            to = recipient.recall<DID>("peerDid"),
//            body = """{"response_requested": true}"""
//        )

        println("Message after encrypting:")
        println(EdgeAgent.packMessage(message, forward = true))
        println(EdgeAgent.packMessage(message, forward = false))

        val forwardMessage = ForwardMessage(
            body = ForwardMessage.ForwardBody(message.to.toString()),
            encryptedMessage = EdgeAgent.packMessage(message, forward = true),
            from = message.from!!,
            to = Environments.MEDIATOR_PEER_DID,
        ).makeMessage()

        println("Forward message:")
        println(forwardMessage)

        recipient.attemptsTo(
            SendDidcommMessage(forwardMessage, forward = false)
        )

    }

    @When("{actor} sends a status-request message")
    fun recipientSendsAStatusRequestMessage(recipient: Actor) {
        recipient.attemptsTo(
            SendDidcommMessage(
                Message1(
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
        val didcommMessage = EdgeAgent.unpackLastDidcommMessage()
        println(didcommMessage)
//        TODO("Not yet implemented https://input-output.atlassian.net/browse/ATL-4883")
    }

    @When("{actor} sends a delivery-request message")
    fun recipientSendsADeliveryRequestMessage(recipient: Actor) {
        recipient.attemptsTo(
            SendDidcommMessage(
                Message1(
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

        val size = didcommMessage.attachments.size
        println("size: $size")

        val data = didcommMessage.attachments.first().data as AttachmentBase64

//        val runner = PickupRunner(didcommMessage, EdgeAgent.mercury)
//
//
//        // why this function is suspended???
//        val result: Array<Pair<String, Message>> = runBlocking {
//            runner.run()
//        }
//
//
//        println("result:")
//        println(result)

//        println(data.base64)
        val decoded = Base64.getUrlDecoder().decode(data.base64).decodeToString()
        println("whats in the attachments:")
        println(decoded)
        val initialMessage = EdgeAgent.unpackMessage(decoded,  true)
//        println(initialMessage)
    }
}