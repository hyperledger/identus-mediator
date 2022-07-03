use lazy_static::lazy_static;
use serde_json::json;

use didcomm::secrets::{Secret, SecretMaterial, SecretType};

lazy_static! {

    pub static ref SCALA_ALICE_SECRET_KEY_X25519: Secret = Secret {
        id: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y".into(),
        type_: SecretType::JsonWebKey2020,
        secret_material: SecretMaterial::JWK {
            private_key_jwk: json!({
                "kty": "OKP",
                "d": "Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c",
                "crv": "X25519",
                "x": "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
            }
        )
        },
    };

    pub static ref SCALA_ALICE_SECRET_KEY_ED25519: Secret = Secret {
        id: "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd".into(),
        type_: SecretType::JsonWebKey2020,
        secret_material: SecretMaterial::JWK {
            private_key_jwk: json!({
                "kty": "OKP",
                "d": "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
                "crv": "Ed25519",
                "x": "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
            })
        },
    };

    // X25519 keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
    // Ed25519 keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")


    pub static ref SCALA_ALICE_SECRETS: Vec<Secret> = vec![
        SCALA_ALICE_SECRET_KEY_X25519.clone(),
        SCALA_ALICE_SECRET_KEY_ED25519.clone(),
    ];
}
