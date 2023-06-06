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

open class SendDidcommMessage(val message: Message): Interaction {
    override fun <T : Actor> performAs(actor: T) {
        val packedMessage = packMessage(message)
        val spec = RequestSpecBuilder().noContentType()
            .setContentType("application/didcomm-encrypted+json")
            .setConfig(RestAssured.config()
                .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)
                    .encodeContentTypeAs("*/*", ContentType.JSON)))
            .setBody(packedMessage)
            .build()
        Post.to("/").with {
            it.spec(spec)
        }.performAs(actor)
    }
}
