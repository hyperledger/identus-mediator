package fmgp.did.resolver.uniresolver

import zio.json._
import zio.json.ast.Json

import fmgp.crypto._
import fmgp.did._
import fmgp.did.comm._

object UniresolverExamples {

  val ex_did_ion = "did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w"

  val ex_did_ion_out = """{
    |  "@context": "https://w3id.org/did-resolution/v1",
    |  "didDocument": {
    |    "id": "did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w",
    |    "@context": [
    |      "https://www.w3.org/ns/did/v1",
    |      {
    |        "@base": "did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w"
    |      }
    |    ],
    |    "service": [
    |      {
    |        "id": "#linkedin",
    |        "type": "linkedin",
    |        "serviceEndpoint": "linkedin.com/in/henry-tsai-6b884014"
    |      },
    |      {
    |        "id": "#github",
    |        "type": "github",
    |        "serviceEndpoint": "github.com/thehenrytsai"
    |      }
    |    ],
    |    "verificationMethod": [
    |      {
    |        "id": "#someKeyId",
    |        "controller": "did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w",
    |        "type": "EcdsaSecp256k1VerificationKey2019",
    |        "publicKeyJwk": {
    |          "kty": "EC",
    |          "crv": "secp256k1",
    |          "x": "WfY7Px6AgH6x-_dgAoRbg8weYRJA36ON-gQiFnETrqw",
    |          "y": "IzFx3BUGztK0cyDStiunXbrZYYTtKbOUzx16SUK0sAY"
    |        }
    |      }
    |    ],
    |    "authentication": [
    |      "#someKeyId"
    |    ]
    |  },
    |  "didResolutionMetadata": {
    |    "contentType": "application/did+ld+json",
    |    "pattern": "^(did:ion:(?!test).+)$",
    |    "driverUrl": "http://driver-did-ion:8080/1.0/identifiers/",
    |    "duration": 12,
    |    "did": {
    |      "didString": "did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w",
    |      "methodSpecificId": "EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w",
    |      "method": "ion"
    |    }
    |  },
    |  "didDocumentMetadata": {
    |    "method": {
    |      "published": true,
    |      "recoveryCommitment": "EiDKYXZ2MkHRCYDVtXI7ONiTkTdVfs9Tnb-tDDHGXLzmOw",
    |      "updateCommitment": "EiDNk40DUvxCef8_BinU5DDIAhNWE4e7Ea9Q6P7GAbJ6VA"
    |    },
    |    "canonicalId": "did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w"
    |  }
    |}""".stripMargin

  val ex_did_ion_out_expected =
    DIDResolutionResult(
      "https://w3id.org/did-resolution/v1",
      DIDDocumentClass(
        DIDSubject("did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w"),
        None,
        None,
        Some(
          Set(
            VerificationMethodEmbeddedJWK(
              "#someKeyId",
              "did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w",
              "EcdsaSecp256k1VerificationKey2019",
              ECPublicKey(
                KTY.EC,
                Curve.secp256k1,
                "WfY7Px6AgH6x-_dgAoRbg8weYRJA36ON-gQiFnETrqw",
                "IzFx3BUGztK0cyDStiunXbrZYYTtKbOUzx16SUK0sAY",
                None
              )
            )
          )
        ),
        Some(List(VerificationMethodReferenced("#someKeyId"))),
        None,
        None,
        None,
        None,
        Some(
          Set(
            DIDServiceGeneric("#linkedin", "linkedin", Json.Str("linkedin.com/in/henry-tsai-6b884014"), None, None),
            DIDServiceGeneric("#github", "github", Json.Str("github.com/thehenrytsai"), None, None)
          )
        )
      ),
      """{"contentType":"application/did+ld+json","pattern":"^(did:ion:(?!test).+)$","driverUrl":"http://driver-did-ion:8080/1.0/identifiers/","duration":12,"did":{"didString":"did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w","methodSpecificId":"EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w","method":"ion"}}"""
        .fromJson[Json]
        .getOrElse(???),
      """{"method":{"published":true,"recoveryCommitment":"EiDKYXZ2MkHRCYDVtXI7ONiTkTdVfs9Tnb-tDDHGXLzmOw","updateCommitment":"EiDNk40DUvxCef8_BinU5DDIAhNWE4e7Ea9Q6P7GAbJ6VA"},"canonicalId":"did:ion:EiClkZMDxPKqC9c-umQfTkR8vvZ9JPhl_xLDI9Nfk38w5w"}"""
        .fromJson[Json]
        .getOrElse(???)
    )

}
