package common

import io.iohk.atala.prism.walletsdk.apollo.ApolloImpl
import io.iohk.atala.prism.walletsdk.castor.CastorImpl
import io.iohk.atala.prism.walletsdk.domain.models.*
import io.iohk.atala.prism.walletsdk.mercury.MercuryImpl
import io.iohk.atala.prism.walletsdk.mercury.resolvers.DIDCommWrapper
import io.iohk.atala.prism.walletsdk.pluto.PlutoImpl
import io.iohk.atala.prism.walletsdk.pluto.data.DbConnection
import io.iohk.atala.prism.walletsdk.prismagent.DIDCOMM1
import io.iohk.atala.prism.walletsdk.prismagent.DIDCOMM_MESSAGING
import kotlinx.coroutines.runBlocking

object EdgeAgent {
    val apollo = ApolloImpl()
    val pluto = PlutoImpl(DbConnection())
    val castor = CastorImpl(apollo)
    val mercury = MercuryImpl(
        castor,
        DIDCommWrapper(castor, pluto, apollo),
        ApiImpl(httpClient())
    )

    fun unpackMessage(message: String): Message {
        return mercury.unpackMessage(message)
    }

    fun packMessage(message: Message): String {
        return mercury.packMessage(message)
    }

    fun createPeerDid(serviceEndpoint: String): DID {
        val seed = apollo.createSeed(apollo.createRandomMnemonics(), "")
        val keyAgreementKeyPair =
            apollo.createKeyPair(seed = seed, curve = KeyCurve(Curve.X25519))
        val authenticationKeyPair =
            apollo.createKeyPair(seed = seed, curve = KeyCurve(Curve.ED25519))
        val did = castor.createPeerDID(
            arrayOf(authenticationKeyPair, keyAgreementKeyPair),
            arrayOf(
                DIDDocument.Service(
                    DIDCOMM1,
                    arrayOf(DIDCOMM_MESSAGING),
                    DIDDocument.ServiceEndpoint(serviceEndpoint)
                )
            )
        )

        val document = runBlocking {
            castor.resolveDID(did.toString())
        }

        val listOfVerificationMethods: MutableList<DIDDocument.VerificationMethod> =
            mutableListOf()
        document.coreProperties.forEach {
            if (it is DIDDocument.Authentication) {
                listOfVerificationMethods.addAll(it.verificationMethods)
            }
            if (it is DIDDocument.KeyAgreement) {
                listOfVerificationMethods.addAll(it.verificationMethods)
            }
        }
        val verificationMethods =
            DIDDocument.VerificationMethods(listOfVerificationMethods.toTypedArray())

        verificationMethods.values.forEach {
            if (it.type.contains("X25519")) {
                pluto.storePrivateKeys(keyAgreementKeyPair.privateKey, did, 0, it.id.toString())
            } else if (it.type.contains("Ed25519")) {
                pluto.storePrivateKeys(
                    authenticationKeyPair.privateKey,
                    did,
                    0,
                    it.id.toString()
                )
            }
        }
        return did
    }

    init {
        runBlocking {
            pluto.start()
        }
    }
}
