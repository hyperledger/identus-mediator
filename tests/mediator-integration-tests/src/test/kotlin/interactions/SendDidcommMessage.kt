package interactions

import net.serenitybdd.screenplay.Actor

import common.EdgeAgent.packMessage
import io.iohk.atala.prism.walletsdk.domain.models.Message
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.EncoderConfig
import io.restassured.http.ContentType
import net.serenitybdd.screenplay.Interaction
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.interactions.RestInteraction

open class SendDidcommMessage(
    val message: Message,
    val contentType: String = "application/didcomm-encrypted+json"): Interaction {
    override fun <T : Actor> performAs(actor: T) {
        val packedMessage = packMessage(message)
        // We have to rewrite spec to remove all unnecessary hardcoded headers
        // from standard serenity rest interaction
        val spec = RequestSpecBuilder().noContentType()
            .setContentType(contentType)
            .setConfig(RestAssured.config()
                .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
            .setBody(packedMessage)
            .build()
        Post.to("/").with {
            it.spec(spec)
        }.performAs(actor)
    }
}
