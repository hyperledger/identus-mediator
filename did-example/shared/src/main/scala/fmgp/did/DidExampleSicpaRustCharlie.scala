package fmgp.did

import zio.json._
import fmgp.crypto._
import fmgp.did._
import zio.json.ast.Json

object DidExampleSicpaRustCharlie {

  // #################################
  // ### did:example:charlie Key ###
  // #################################

  def charlieKeyX25519 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:charlie#key-x25519-1"),
    crv = Curve.X25519,
    d = "Z-BsgFe-eCvhuZlCBX5BV2XiDE2M92gkaORCe68YdZI",
    x = "nTiVFj7DChMsETDdxd5dIzLAJbSQ4j4UG6ZU1ogLNlw"
  )

  def charlieKey1 = OKPPrivateKey(
    kty = KTY.OKP,
    kid = Some("did:example:charlie#key-1"),
    crv = Curve.Ed25519,
    d = "T2azVap7CYD_kB8ilbnFYqwwYb5N-GcD6yjGEvquZXg",
    x = "VDXDwuGKVq91zxU6q7__jLDUq8_C5cuxECgd-1feFTE"
  )

  // // // #######################################################

  val charlieSecrets = KeyStore(
    Set(
      charlieKeyX25519,
      charlieKey1,
    )
  )

  val charlieDIDDocument =
    DIDDocumentClass(
      // "@context":["https://www.w3.org/ns/did/v2"],
      id = DIDSubject("did:example:charlie"),
      authentication = Some(Seq(VerificationMethodReferenced(charlieKey1.kid.get))),
      keyAgreement = Some(Set(VerificationMethodReferenced(charlieKeyX25519.kid.get))),
      verificationMethod = Some(
        Set(charlieKeyX25519, charlieKey1).map(e =>
          VerificationMethodEmbeddedJWK(
            id = e.kid.get,
            controller = e.kid.get,
            `type` = "JsonWebKey2020",
            publicKeyJwk = e.toPublicKey.copy(kid = None)
          )
        )
      ),
      service = Some(
        Set(
          DIDServiceGeneric(
            id = "did:example:charlie#didcomm-1",
            `type` = "DIDCommMessaging",
            serviceEndpoint = Json.Arr(
              Json.Obj(
                ("uri", Json.Str("did:example:mediator3")),
                ("accept", Json.Arr(Json.Str("didcomm/v2"), Json.Str("didcomm/aip2;env=rfc587"))),
                (
                  "routing_keys",
                  Json.Arr(
                    Json.Str("did:example:mediator2#key-x25519-1"),
                    Json.Str("did:example:mediator1#key-x25519-1")
                  )
                ),
              )
            )
          )
        )
      )
    )

}
