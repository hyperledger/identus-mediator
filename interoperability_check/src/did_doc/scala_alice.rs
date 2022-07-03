use lazy_static::lazy_static;
use serde_json::json;

use didcomm::did::{
    DIDCommMessagingService, DIDDoc, Service, ServiceKind, VerificationMaterial,
    VerificationMethod, VerificationMethodType,
};

lazy_static! {
    // {
    //     "id" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
    //     "authentication" : [
    //       {
    //         "id" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd",
    //         "controller" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
    //         "type" : "JsonWebKey2020",
    //         "publicKeyJwk" : {
    //           "kty" : "OKP",
    //           "crv" : "Ed25519",
    //           "x" : "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA"
    //         }
    //       }
    //     ],
    //     "keyAgreement" : [
    //       {
    //         "id" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y",
    //         "controller" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
    //         "type" : "JsonWebKey2020",
    //         "publicKeyJwk" : {
    //           "kty" : "OKP",
    //           "crv" : "X25519",
    //           "x" : "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"
    //         }
    //       }
    //     ],
    //     "service" : [
    //       {
    //         "id" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#didcommmessaging-0",
    //         "type" : [
    //           "DIDCommMessaging"
    //         ],
    //         "serviceEndpoint" : "https://alice.did.fmgp.app/",
    //         "accept" : [
    //           "didcomm/v2"
    //         ]
    //       }
    //     ]
    //   }

    pub static ref  SCALA_ALICE_DID_COMM_MESSAGING_SERVICE: DIDCommMessagingService =
      DIDCommMessagingService {
        uri: "https://alice.did.fmgp.app/".into(),
        accept: Some(vec!["didcomm/v2".into()]), //"didcomm/aip2;env=rfc587".into()
        routing_keys: vec![], //"did:example:mediator1#key-x25519-1".into()
      };
    pub static ref  SCALA_ALICE_SERVICE: Service = Service {
      id: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#didcommmessaging-0".into(),
      service_endpoint: ServiceKind::DIDCommMessaging {
          value:  SCALA_ALICE_DID_COMM_MESSAGING_SERVICE.clone()
        },
      };
      pub static ref SCALA_ALICE_DID_DOC: DIDDoc = DIDDoc {
        id: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ".into(),
        authentication: vec![
          "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd".into(),
        ],
        key_agreement: vec![
          "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y".into(),
        ],
        service: vec![SCALA_ALICE_SERVICE.clone()],
        verification_method: vec![
          VerificationMethod {
            id: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y".into(),
            controller: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ".into(),
            type_: VerificationMethodType::JsonWebKey2020,
            verification_material: VerificationMaterial::JWK {
              public_key_jwk: json!({
                "kty": "OKP",
                "crv": "X25519",
                "x": "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
              })
            },
          },
          VerificationMethod {
            id: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd".into(),
            controller: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ".into(),
            type_: VerificationMethodType::JsonWebKey2020,
            verification_material: VerificationMaterial::JWK {
              public_key_jwk: json!({
                "kty": "OKP",
                "crv": "Ed25519",
                "x": "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
              })
            },
          },

      ],
    };
}
