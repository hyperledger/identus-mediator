package interactions

import net.serenitybdd.screenplay.Actor

import common.EdgeAgent.packMessage
import common.TestConstants
import io.iohk.atala.prism.walletsdk.domain.models.Message
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.EncoderConfig
import net.serenitybdd.core.Serenity
import net.serenitybdd.screenplay.Interaction
import net.serenitybdd.screenplay.rest.interactions.Post

open class SendDidcommMessage(
    val message: Message,
    val contentType: String = TestConstants.DIDCOMM_V2_CONTENT_TYPE_ENCRYPTED,
    val forward: Boolean = false
): Interaction {
    override fun <T : Actor> performAs(actor: T) {
        Serenity.recordReportData().withTitle("DIDComm Message").andContents(
            message.toJsonString()
        )
        val packedMessage = packMessage(message, forward)
        // We have to rewrite spec to remove all unnecessary hardcoded headers
        // from standard serenity rest interaction
        val spec = RequestSpecBuilder().noContentType()
            .setContentType(contentType)
            .setConfig(RestAssured.config()
                .encoderConfig(
                    EncoderConfig
                        .encoderConfig()
                        .appendDefaultContentCharsetToContentTypeIfUndefined(false))
            )
            .setBody(packedMessage)
            .build()
        Post.to("/").with {
            it.spec(spec)
        }.performAs(actor)
    }
}
