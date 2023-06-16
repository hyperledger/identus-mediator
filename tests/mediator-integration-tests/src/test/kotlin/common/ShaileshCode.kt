package common

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import org.didcommx.didcomm.common.VerificationMaterial
import org.didcommx.didcomm.common.VerificationMaterialFormat
import org.didcommx.didcomm.common.VerificationMethodType
import org.didcommx.didcomm.diddoc.DIDDocResolver
import org.didcommx.didcomm.secret.Secret
import org.didcommx.didcomm.secret.SecretResolverInMemory
import org.didcommx.peerdid.core.*

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import org.didcommx.didcomm.diddoc.DIDCommService
import org.didcommx.didcomm.diddoc.DIDDoc
import org.didcommx.didcomm.diddoc.VerificationMethod
import org.didcommx.didcomm.utils.toJson
import org.didcommx.peerdid.*
import java.util.*



abstract class Listener {

    private var receivedResponse: String? = null

    fun route(application: Application) {
        application.routing {
            post("/") {
                val json = call.receiveText()
                receivedResponse = json
                call.respond(HttpStatusCode.OK, "Data received")
            }
        }
    }

    fun startListening(port: Int = 9999) {
        embeddedServer(Netty, port = 9999, module = {route(this)}).start(wait = false)
    }

    fun receivedResponse(): String? {
        return receivedResponse
    }

}

data class DidId(val value: String)

interface DidAgent {
    val did: DidId
    val jwkForKeyAgreement: List<OctetKeyPair>
    val jwkForKeyAuthentication: List<OctetKeyPair>
}


data class PeerDID(
    override val did: DidId,
    override val jwkForKeyAgreement: List<OctetKeyPair>,
    override val jwkForKeyAuthentication: List<OctetKeyPair>,
) : DidAgent, Listener() {
    val didDocument: String
        get() = org.didcommx.peerdid.resolvePeerDID(did.value, VerificationMaterialFormatPeerDID.JWK)

    fun getSecretResolverInMemory(): SecretResolverInMemory {

        fun validateRawKeyLength(key: ByteArray) {
            // for all supported key types now (ED25519 and X25510) the expected size is 32
            if (key.size != 32)
                throw IllegalArgumentException("Invalid key $key")
        }

        fun createMultibaseEncnumbasis(key: VerificationMaterialPeerDID<out VerificationMethodTypePeerDID>): String {
            val decodedKey = when (key.format) {
                VerificationMaterialFormatPeerDID.BASE58 -> fromBase58(key.value.toString())
                VerificationMaterialFormatPeerDID.MULTIBASE -> fromMulticodec(fromBase58Multibase(key.value.toString()).second).second
                VerificationMaterialFormatPeerDID.JWK -> fromJwk(key)
            }
            validateRawKeyLength(decodedKey)
            return toBase58Multibase(toMulticodec(decodedKey, key.type))
        }

        val keyAgreement =
            AgentPeerService.keyAgreemenFromPublicJWK(this.jwkForKeyAgreement.first()) // TODO Fix first()
        val keyAuthentication =
            AgentPeerService.keyAuthenticationFromPublicJWK(this.jwkForKeyAuthentication.first()) // TODO Fix first()

        val keyIdAgreement = createMultibaseEncnumbasis(keyAgreement).drop(1)
        val keyIdAuthentication = createMultibaseEncnumbasis(keyAuthentication).drop(1)

        val secretKeyAgreement = Secret(
            "${this.did.value}#$keyIdAgreement",
            VerificationMethodType.JSON_WEB_KEY_2020,
            VerificationMaterial(VerificationMaterialFormat.JWK, this.jwkForKeyAgreement.first().toJSONString())
        )
        val secretKeyAuthentication = Secret(
            "${this.did.value}#$keyIdAuthentication",
            VerificationMethodType.JSON_WEB_KEY_2020,
            VerificationMaterial(VerificationMaterialFormat.JWK, this.jwkForKeyAuthentication.first().toJSONString())
        )

        return SecretResolverInMemory(
            mutableMapOf(
                "${this.did.value}#$keyIdAgreement" to secretKeyAgreement,
                "${this.did.value}#$keyIdAuthentication" to secretKeyAuthentication,
            ).toMap()
        )
    }

    fun getDidDocResolverInMemory(): DIDDocResolver {
        return DIDDocResolverPeerDID()
    }

    fun getServiceEndpoint(): String {
        return DIDDocResolverPeerDID().resolve(this.did.value).get().didCommServices.map { it.serviceEndpoint }.first()
    }
}

object AgentPeerService {

    fun createServiceJson(serviceEndpoint: String): String {
        return """
        {
            "type": "DIDCommMessaging",
            "serviceEndpoint": "$serviceEndpoint",
            "routingKeys": [],
            "accept": ["didcomm/v2"]
        }
    """
    }

    fun makeNewJwkKeyX25519(): OctetKeyPair = OctetKeyPairGenerator(Curve.X25519).generate()
    fun makeNewJwkKeyEd25519(): OctetKeyPair = OctetKeyPairGenerator(Curve.Ed25519).generate()
    fun keyAgreemenFromPublicJWK(key: OctetKeyPair): VerificationMaterialPeerDID<VerificationMethodTypeAgreement> =
        VerificationMaterialPeerDID(
            VerificationMaterialFormatPeerDID.JWK,
            key.toPublicJWK(),
            VerificationMethodTypeAgreement.JSON_WEB_KEY_2020
        )

    fun keyAuthenticationFromPublicJWK(key: OctetKeyPair): VerificationMaterialPeerDID<VerificationMethodTypeAuthentication> =
        VerificationMaterialPeerDID(
            VerificationMaterialFormatPeerDID.JWK,
            key.toPublicJWK(),
            VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020
        )

    fun makePeerDid(
        jwkForKeyAgreement: OctetKeyPair = makeNewJwkKeyX25519(),
        jwkForKeyAuthentication: OctetKeyPair = makeNewJwkKeyEd25519(),
        serviceEndpoint: String? = null
    ): PeerDID {
        println("**************************jwkForKeyAgreement***********************************************************")
        println(jwkForKeyAgreement)
        println("**************************jwkForKeyAgreement***********************************************************")
        println("************************************jwkForKeyAuthentication*************************************************")
        println(jwkForKeyAuthentication)
        println("************************************jwkForKeyAuthentication*************************************************")

        val did = org.didcommx.peerdid.createPeerDIDNumalgo2(
            listOf(keyAgreemenFromPublicJWK(jwkForKeyAgreement)),
            listOf(keyAuthenticationFromPublicJWK(jwkForKeyAuthentication)),
            serviceEndpoint?.let { createServiceJson(serviceEndpoint) }
        )
        return PeerDID(DidId(did), listOf(jwkForKeyAgreement), listOf(jwkForKeyAuthentication))
    }

    fun makeAgent(
        serviceEndpoint: String? = null
    ): PeerDID {
        return makePeerDid(
            makeNewJwkKeyX25519(),
            makeNewJwkKeyEd25519(),
            serviceEndpoint = serviceEndpoint
        )
    }

    fun makeAgentFromPeerDid(
        did: String
    ): PeerDID {
        return PeerDID(DidId(did), listOf(), listOf())
    }
}

class DIDDocResolverPeerDID : DIDDocResolver {

    override fun resolve(did: String): Optional<DIDDoc> {
        val didDocJson = resolvePeerDID(did, format = VerificationMaterialFormatPeerDID.JWK)
        val didDoc = DIDDocPeerDID.fromJson(didDocJson)
        return Optional.ofNullable(
            DIDDoc(
                did = did,
                keyAgreements = didDoc.agreementKids,
                authentications = didDoc.authenticationKids,
                verificationMethods = (didDoc.authentication + didDoc.keyAgreement).map {
                    VerificationMethod(
                        id = it.id,
                        type = VerificationMethodType.JSON_WEB_KEY_2020,
                        controller = it.controller,
                        verificationMaterial = VerificationMaterial(
                            format = VerificationMaterialFormat.JWK,
                            value = toJson(it.verMaterial.value)
                        )
                    )
                },
                didCommServices = didDoc.service?.mapNotNull {
                    when (it) {
                        is DIDCommServicePeerDID ->
                            DIDCommService(
                                id = it.id,
                                serviceEndpoint = it.serviceEndpoint,
                                routingKeys = it.routingKeys,
                                accept = it.accept
                            )

                        else -> null
                    }
                }
                    ?: emptyList()
            )
        )
    }
}
