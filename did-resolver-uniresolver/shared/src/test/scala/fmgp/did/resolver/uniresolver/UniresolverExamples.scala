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
            DIDServiceGeneric("#linkedin", "linkedin", "linkedin.com/in/henry-tsai-6b884014", None, None),
            DIDServiceGeneric("#github", "github", "github.com/thehenrytsai", None, None)
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

  val ex_did_com = "did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr"
  val ex_did_com_out = """{
    |  "@context": "https://w3id.org/did-resolution/v1",
    |  "didDocument": {
    |    "@context": [
    |      "https://www.w3.org/ns/did/v1"
    |    ],
    |    "id": "did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr",
    |    "verificationMethod": [
    |      {
    |        "ID": "did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr#keys-1",
    |        "Type": "RsaVerificationKey2018",
    |        "Controller": "did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr",
    |        "publicKeyMultibase": "mMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqOnHy8jZsQgwvzWh+1BiG/+ZsNeYfGohZWneoCdGJGTF5hRO6uzTGSJr0j0K2mcxF3euTl6pQaeRN8PXCzmllMlFkSiATls4PT1fUM2wjDaKIZLcfJGE8Z8+eAxow4soF4/yGLyO5KOv7/uAdcF8FwQ+2kZyO3ZFpeKR1dmver4/VolUOJ3VNioxYXVnxx2S96M1UsgslCdWg/VAR9b2MoDKGHTWbidiCwogFywc604cIXgX6hzAS5JJcFD2bMk7mrFZQPhSwMfvGBdtyE1RANkfHdr18EBZra3o+wnCz1OAtNzdmyL85jU9cKXxd/MtAs2bts/wx65DrXWJ1uAsHwIDAQAB"
    |      },
    |      {
    |        "ID": "did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr#keys-2",
    |        "Type": "RsaSignatureKey2018",
    |        "Controller": "did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr",
    |        "publicKeyMultibase": "mMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhbDUHdyvTxYs9JGNWzNybOLoRgjCNabjSssrkyRQ2rgLwJftwRv1RPdg+Zqwav5d2Buiy5XGXAtjR/GXirWZ187Jh4H5SYtnPeClF9r8hrNoO4fPDOic5+L4/nbkaB1hQuVc9xSe8qARHYpyUtKAJOog85NnTnAz0PPlUMf8Y782you3VAtQwrtNvcF5yt1euc/HnqPL2wTtKg/knYKtKyTEsxtADpFs8hiYW6I0OC2Na/Q+93mQAMUQV38o6b1A68SVokYbKe+3/ezPY7UYZzsB6xdv5s+aNPtyuRqsOSGS9IK+k42ukp1FK4XQxfX1vfcpgp8BVMdlbZ7l1OWAbQIDAQAB"
    |      }
    |    ]
    |  },
    |  "didResolutionMetadata": {
    |    "contentType": "application/did+ld+json",
    |    "pattern": "^(did:com:.+)$",
    |    "driverUrl": "http://driver-did-com:8080/1.0/identifiers/",
    |    "duration": 505,
    |    "did": {
    |      "didString": "did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr",
    |      "methodSpecificId": "17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr",
    |      "method": "com"
    |    }
    |  },
    |  "didDocumentMetadata": {
    |    "created": "2021-01-06T00:05:59Z",
    |    "updated": "2021-01-06T00:05:59Z"
    |  }
    |}""".stripMargin
}
