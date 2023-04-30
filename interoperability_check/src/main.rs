// #[macro_use(lazy_static)]
extern crate lazy_static;

use didcomm::algorithms::{AnonCryptAlg, AuthCryptAlg};
// use didcomm::protocols::routing::try_parse_forward;
use didcomm::{
    did::resolvers::ExampleDIDResolver, secrets::resolvers::ExampleSecretsResolver, Message,
};
use didcomm::{PackEncryptedOptions, UnpackOptions};
use serde_json::json;

mod did_doc;
mod secrets;
use crate::did_doc::*;
use crate::secrets::*;

#[tokio::main(flavor = "current_thread")]
async fn main() {
    println!("Hello, world!");
    println!("=================== NON REPUDIABLE ENCRYPTION ===================");
    non_repudiable_encryption().await;
    println!("=================== Message FROM alice TO alice.did.fmgp.app ===================");
    send_to_scala_alice().await;
    println!("=================== Message FROM alice.did.fmgp.app TO bob ===================");
    read_from_scala_alice().await;
    println!("=================== Message FROM alice.did.fmgp.app TO alice.did.fmgp.app ===================");
    read_from_scala_alice_to_scala_alice().await;
    println!("=================== Message FROM alice TO bob ===================");
    bob_read_message().await;
}

async fn send_to_scala_alice() {
    // --- Building message from ALICE to BOB ---
    let msg = Message::build(
        "example-1".to_owned(),
        "example/v1".to_owned(),
        json!({"aaa":"example-body"}),
    )
    .to(SCALA_ALICE_DID_DOC.id.to_owned())
    .from(ALICE_DID_DOC.id.to_owned())
    .finalize();

    // --- Packing encrypted and authenticated message ---
    let did_resolver =
        ExampleDIDResolver::new(vec![ALICE_DID_DOC.clone(), SCALA_ALICE_DID_DOC.clone()]);

    let secrets_resolver = ExampleSecretsResolver::new(ALICE_SECRETS.clone());
    let (msg2, metadata) = msg
        .pack_encrypted(
            &SCALA_ALICE_DID_DOC.id,
            Some(&ALICE_DID_DOC.id),
            None, //Some(ALICE_DID),
            &did_resolver,
            &secrets_resolver,
            // &PackEncryptedOptions::default(),
            &PackEncryptedOptions {
                protect_sender: false,
                forward: false,
                forward_headers: None,
                messaging_service: None,
                enc_alg_auth: AuthCryptAlg::default(),
                enc_alg_anon: AnonCryptAlg::default(),
            },
        )
        .await
        .expect("Unable pack_encrypted");

    println!("Encryption metadata is\n{:?}\n", metadata);

    // --- Sending message by Alice ---
    println!("Alice is sending message \n{}\n", msg2);

    // --- Unpacking message by Bob ---
    let did_resolver =
        ExampleDIDResolver::new(vec![ALICE_DID_DOC.clone(), SCALA_ALICE_DID_DOC.clone()]);

    let secrets_resolver2 = ExampleSecretsResolver::new(SCALA_ALICE_SECRETS.clone());

    let (msg3, metadata3) = Message::unpack(
        &msg2,
        &did_resolver,
        &secrets_resolver2,
        &UnpackOptions::default(),
    )
    .await
    .expect("Unable unpack");

    println!("Received message is \n{:?}\n", msg3);
    println!("Received message unpack metadata is \n{:?}\n", metadata3);
}

// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

async fn read_from_scala_alice() {
    // --- Building message from ALICE to BOB ---

    // let msg = "{\"ciphertext\":\"cEk34jhpsG1h0lXwyHu1lmGJR6KJLcxDCou9Fgdy9oL1fff0Hesg9Dr-wPi6OGZOhVv5k-NoPeqAXagHZ-usQwKxJQKI6jZyXquABYHUr0_pzDw692SjNp2LXAjwA28UyPPGeH4Cohku_ELKEBMQxtNb5RT6mdEiLQbQWWlRYWt6hWY9EKpxsPiBfHLaXh9PImhZLNx-e2_NnmKn44bNZc3D7YDm8R73X7Wz8SUqTXIQOpGhrVjS3lTDf7Ck_0OpsDvvUdyQ5XSv5-TfDGX4V0oYpDcwfIF4g_IRU9ByleAORKc0RtgcG7xlfayG66ctr5pqcjd5QAcRUA5QLumLagLfRaSVj828CIni3czrk39WGE9YhsS4W4Br6kGz_YQp0T2ulQojyzdcXNZ6Hy-sO1TnWZjb2vN7Rg9RE1GZQGeM1FRtOoUNzRav7LcsbfgjkXEoMYYsncwQcBC77GkMs3peLk6IwuncY6zd_X1WS1bcpQIw7x56V5q-s2Vvmuka\",\"protected\":\"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IlRKTEpWWFQtQ0hVVTV5bEtMbnQ0ZEFvWTB1N2lQa0lQUzdpSThGQXMtUVUifSwiYXB2IjoiNjhTNnpJVjlGYVVtVDlXSDlHZXRrelh4WGVpclU0T0N0OXpfclotcEo1cyIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWhiR2xqWlM1a2FXUXVabTFuY0M1aGNIQXZJaXdpY2lJNlcxMHNJbUVpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmUSM2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eSIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJkb2QxTkZORE0zZDI1RVJURndkRE5ZTm1oV1JGVlJlbE5xYzBoNmFXNXdXRE5ZUm5aTmFsSkJiVGQ1TGxaNk5rMXJhR2d4WlRWRFJWbFpjVFpLUWxWalZGbzJRM0F5Y21GdVExZFNjblkzV1dGNE0weGxORTQxT1ZJMlpHUXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRsb1lrZHNhbHBUTld0aFYxRjFXbTB4Ym1ORE5XaGpTRUYyU1dsM2FXTnBTVFpYTVRCelNXMUZhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxFak5reFRaMmgzVTBVME16ZDNia1JGTVhCME0xZzJhRlpFVlZGNlUycHpTSHBwYm5CWU0xaEdkazFxVWtGdE4zayIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0\",\"recipients\":[{\"encrypted_key\":\"_TKGujxLuQZ7cUoENTgIwn7IY4CuTkBIL96qhL7I54GnN8qbwJxchrAVRsvI9Ewu6BxbqbmUdg9LPxisu58QPfzYIOFPIyKs\",\"header\":{\"kid\":\"did:example:bob#key-x25519-2\"}},{\"encrypted_key\":\"PAvmXXISCvjb3Ux6-c7koW8XRW5vQq6UhEZtoySCi_fPb6WVYrR9RACFQ5ZoONUG0QVlGV0U7AHXbXGqRy3F7VhtLMXzoRwY\",\"header\":{\"kid\":\"did:example:bob#key-x25519-1\"}},{\"encrypted_key\":\"7lmlgoiRLYISSp3inWaXY_cm-kbJMkOLZe_DyuXO3Jxp97NL_8xD7p8TXm9mCsnGd3tCvxfBc5G9v1oqDtRtyOje96EukSjs\",\"header\":{\"kid\":\"did:example:bob#key-x25519-not-secrets-1\"}},{\"encrypted_key\":\"7lmlgoiRLYISSp3inWaXY_cm-kbJMkOLZe_DyuXO3Jxp97NL_8xD7p8TXm9mCsnGd3tCvxfBc5G9v1oqDtRtyOje96EukSjs\",\"header\":{\"kid\":\"did:example:bob#key-x25519-3\"}}],\"tag\":\"anpaAnlJ58yUuwSpS6XJEAhSbPh55vNCnGuzScjLSmg\",\"iv\":\"XvrZU06_Oe62ofvmQR0kCg\"}";
    let msg = "{
        \"ciphertext\":\"MnxIB613jHU89P17cUVoCSUztFEM0BuZrAhuGQ33jykIDTOssTlOVYlHa3GYzxzXAeycUMvjkvv_uhpeHXgWgtfCs-vk23gbfoJhydYe4W3oDWCmS17fhxYxokKEQpR5nhZ6jQO8e4xrSCGd2_G-jtEEfKgPJYKIfofYaHYqEvZFFzhtiTWUg_-61jkAptyanJ5Kl6KSMey0NZgh72EWPMFmLitla35IJiPCS2-raoHk2N_KH6Xp1nXfJpmypqNU6hdS4BuU5thZWkY-L3Qn0aHb_f_FD52Nn74754OlWE5kPDtsLGo-ik3ca7JdoSrYTYVQnUWoElDqj5EQEK2mcl4pyBDGT8iXqwxBobh91RraLp42PVyVQwyuGVwFNrUR-UB_RCoQdwMsuflw4J-dXBKl0l9wmNR2ZzwA4M-mAu2G_civt8eyKXPHWY1oh20uz07er9cxjcKfBihdA6-ihMiP6fM6WMz50m_XbgikyMY\",
        \"protected\":\"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6ImJlTkJqemJudk9XcFBJbGI2dDRJV1F0VUU2S3pUWlRhWWFwWWRNQ1ZRa28ifSwiYXB2IjoiNjhTNnpJVjlGYVVtVDlXSDlHZXRrelh4WGVpclU0T0N0OXpfclotcEo1cyIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWhiR2xqWlM1a2FXUXVabTFuY0M1aGNIQXZJaXdpY2lJNlcxMHNJbUVpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmUSM2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eSIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJkb2QxTkZORE0zZDI1RVJURndkRE5ZTm1oV1JGVlJlbE5xYzBoNmFXNXdXRE5ZUm5aTmFsSkJiVGQ1TGxaNk5rMXJhR2d4WlRWRFJWbFpjVFpLUWxWalZGbzJRM0F5Y21GdVExZFNjblkzV1dGNE0weGxORTQxT1ZJMlpHUXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRsb1lrZHNhbHBUTld0aFYxRjFXbTB4Ym1ORE5XaGpTRUYyU1dsM2FXTnBTVFpYTVRCelNXMUZhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxFak5reFRaMmgzVTBVME16ZDNia1JGTVhCME0xZzJhRlpFVlZGNlUycHpTSHBwYm5CWU0xaEdkazFxVWtGdE4zayIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0\",
        \"recipients\":[
          {\"encrypted_key\":\"LLOpiwA2aKTedZAomLHIV1QsTXGY0pTIh35XLRrXJmGzYUM9_uPfcAgRejroRC-qJuyWmq6Uk1gZ9g7ym0ijNAjIpbOWWFBA\",\"header\":{\"kid\":\"did:example:bob#key-x25519-not-secrets-1\"}},
          {\"encrypted_key\":\"LLOpiwA2aKTedZAomLHIV1QsTXGY0pTIh35XLRrXJmGzYUM9_uPfcAgRejroRC-qJuyWmq6Uk1gZ9g7ym0ijNAjIpbOWWFBA\",\"header\":{\"kid\":\"did:example:bob#key-x25519-3\"}},
          {\"encrypted_key\":\"MlAY6hzDM2DXPfBD_YmoKLIgsc97GMg2oPEFXPNQRUS5COQ7SOw-l1WgXQYtnFuhHWFKrUdpIm-uIxv8MotXHvlMbnBoJadW\",\"header\":{\"kid\":\"did:example:bob#key-x25519-2\"}},
          {\"encrypted_key\":\"OT4mquEOW0SlozS1e-z_oxhwjmENXq1bGuN3zWCRqSvzrjZprTQd4eF0-7W3fisNR3OwQXSCBB6h4bZb4U0wdZfL2kjo0ME1\",\"header\":{\"kid\":\"did:example:bob#key-x25519-1\"}}
        ],
        \"tag\":\"RMCjS3EMI6OMyFCy-YW1KuXjjgeU94KMQswVrA-B-XA\",
        \"iv\":\"VPD4AiKepteFruJWDG5g1Q\"
    }";

    // println!("Encryption metadata is\n{:?}\n", metadata);

    // --- Sending message by Alice ---
    println!("Alice is sending message \n{}\n", msg);

    // --- Unpacking message by Bob ---
    let did_resolver =
        ExampleDIDResolver::new(vec![BOB_DID_DOC.clone(), SCALA_ALICE_DID_DOC.clone()]);

    let secrets_resolver = ExampleSecretsResolver::new(BOB_SECRETS.clone());

    let (msg, metadata) = Message::unpack(
        &msg,
        &did_resolver,
        &secrets_resolver,
        &UnpackOptions::default(),
    )
    .await
    .expect("Unable unpack");

    println!("Received message is \n{:?}\n", msg);
    println!("Received message unpack metadata is \n{:?}\n", metadata);
}

async fn read_from_scala_alice_to_scala_alice() {
    // let msg = "{\"ciphertext\":\"cEk34jhpsG1h0lXwyHu1lmGJR6KJLcxDCou9Fgdy9oL1fff0Hesg9Dr-wPi6OGZOhVv5k-NoPeqAXagHZ-usQwKxJQKI6jZyXquABYHUr0_pzDw692SjNp2LXAjwA28UyPPGeH4Cohku_ELKEBMQxtNb5RT6mdEiLQbQWWlRYWt6hWY9EKpxsPiBfHLaXh9PImhZLNx-e2_NnmKn44bNZc3D7YDm8R73X7Wz8SUqTXIQOpGhrVjS3lTDf7Ck_0OpsDvvUdyQ5XSv5-TfDGX4V0oYpDcwfIF4g_IRU9ByleAORKc0RtgcG7xlfayG66ctr5pqcjd5QAcRUA5QLumLagLfRaSVj828CIni3czrk39WGE9YhsS4W4Br6kGz_YQp0T2ulQojyzdcXNZ6Hy-sO1TnWZjb2vN7Rg9RE1GZQGeM1FRtOoUNzRav7LcsbfgjkXEoMYYsncwQcBC77GkMs3peLk6IwuncY6zd_X1WS1bcpQIw7x56V5q-s2Vvmuka\",\"protected\":\"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IlRKTEpWWFQtQ0hVVTV5bEtMbnQ0ZEFvWTB1N2lQa0lQUzdpSThGQXMtUVUifSwiYXB2IjoiNjhTNnpJVjlGYVVtVDlXSDlHZXRrelh4WGVpclU0T0N0OXpfclotcEo1cyIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWhiR2xqWlM1a2FXUXVabTFuY0M1aGNIQXZJaXdpY2lJNlcxMHNJbUVpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmUSM2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eSIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJkb2QxTkZORE0zZDI1RVJURndkRE5ZTm1oV1JGVlJlbE5xYzBoNmFXNXdXRE5ZUm5aTmFsSkJiVGQ1TGxaNk5rMXJhR2d4WlRWRFJWbFpjVFpLUWxWalZGbzJRM0F5Y21GdVExZFNjblkzV1dGNE0weGxORTQxT1ZJMlpHUXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRsb1lrZHNhbHBUTld0aFYxRjFXbTB4Ym1ORE5XaGpTRUYyU1dsM2FXTnBTVFpYTVRCelNXMUZhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxFak5reFRaMmgzVTBVME16ZDNia1JGTVhCME0xZzJhRlpFVlZGNlUycHpTSHBwYm5CWU0xaEdkazFxVWtGdE4zayIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0\",\"recipients\":[{\"encrypted_key\":\"_TKGujxLuQZ7cUoENTgIwn7IY4CuTkBIL96qhL7I54GnN8qbwJxchrAVRsvI9Ewu6BxbqbmUdg9LPxisu58QPfzYIOFPIyKs\",\"header\":{\"kid\":\"did:example:bob#key-x25519-2\"}},{\"encrypted_key\":\"PAvmXXISCvjb3Ux6-c7koW8XRW5vQq6UhEZtoySCi_fPb6WVYrR9RACFQ5ZoONUG0QVlGV0U7AHXbXGqRy3F7VhtLMXzoRwY\",\"header\":{\"kid\":\"did:example:bob#key-x25519-1\"}},{\"encrypted_key\":\"7lmlgoiRLYISSp3inWaXY_cm-kbJMkOLZe_DyuXO3Jxp97NL_8xD7p8TXm9mCsnGd3tCvxfBc5G9v1oqDtRtyOje96EukSjs\",\"header\":{\"kid\":\"did:example:bob#key-x25519-not-secrets-1\"}},{\"encrypted_key\":\"7lmlgoiRLYISSp3inWaXY_cm-kbJMkOLZe_DyuXO3Jxp97NL_8xD7p8TXm9mCsnGd3tCvxfBc5G9v1oqDtRtyOje96EukSjs\",\"header\":{\"kid\":\"did:example:bob#key-x25519-3\"}}],\"tag\":\"anpaAnlJ58yUuwSpS6XJEAhSbPh55vNCnGuzScjLSmg\",\"iv\":\"XvrZU06_Oe62ofvmQR0kCg\"}";
    //let msg = "{\"ciphertext\":\"TvR9io0KpYmyDA2WU0nUda5PN0HsxlElr_SnHU_nUj9OUYPrjCRtmCzGWaPzRZUJU23M6OXVCYz-HJHxO8bf3snIqrB-WI5x2tEs2Zhki6kV3N_qsgZILL3cSRbYP4nOGL0ZsOKZpIeecATw6_HUY4WEnJQAFsW7f85CNOWHcrjih6aOK2GZraLYA62yDIzmHgx3eBcXtZthNbZYIlpiXHxjLzrgh-72DtCo-rxtdrEPKohu8mAazEltrrK91Ggg2xEodvYx6FKmLvmeApnCD0uZ8vVhH7h15fwGfyjYXuWclEzJLCkEjf_CRPpWFSruZtUlu6qY8oQuOEp2r6JchSGsbzgjI0yqIIGx2alA519xh1GCq2TN_xn_WPU6Q19t4FnEzLhWqbc1JPO40MCwaVR30Vbf9AoQ_MCJywxDpuTVHucvJWkvItKC9rM6lKcutRBcaNGz_kAIBHLSvH7GtlnrhblM-Dw8WhbFAVSJHcEwxOnMDrnUU8Pfs0-uRj3jCfUro_8VA_06NUQTT-T7o7q0P85cdahlG8BNjJJU53dRRRBIRAa8gc3kNz413ptaVtlc13aDUSAwhwoRV1e0sfZfoeX7iZhyYEIr1PKPTRMQqLaLOea2dcsr_j8NlRhjL6lgy-svlAX0LV02tOkHumt6dkJcbiOtD-JY0O6WfS4Ib72MCwi8nA9DHDR1KA1RHla0Hoi20pYGvPqoyqrkWJ4M-VY4zuN_voh0PrvxZjF77giiGbwXj-95GQQNKuB5dsyYL22JiGr6-WSKxBJRd2fCF1ZhJv__U4LEQIZfZHZ33INw-0Ibr74g66HDm-eonJvSNxyJFzDdGW7wW8J7fAOQ-kKcQy0vGyvuyfOvRy17wtV3Y-t0fcMc8RmANykPREaQOo1r8R15mEn5kKU6-SvZBHmZY3iKOIu_XhLCzX1iAa7hviHK_2uUCpCDeLbVSipdBf6Lke7MKnNc_HADLlQ8Zk5r4iLEBCLO89H3TA4YfXaJxLHWFgNnGO5-1p9M9BWoZ5if-uAp8sv4xt9yQHiG4jUkeoSk5Qtezy2g870O-E-DGffEi4-FWIFlz7rp\",\"protected\":\"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IlBLSW5UWTF4R3dMendGQjJQV3ZEZVJRMUtvbDNzUThqQ3N1LVZOVGhIQ3cifSwiYXB2IjoiLWNOQ3l0eFVrSHpSRE5SckV2Vm05S0VmZzhZcUtQVnVVcVg1a0VLbU9yMCIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWhiR2xqWlM1a2FXUXVabTFuY0M1aGNIQXZJaXdpY2lJNlcxMHNJbUVpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmUSM2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eSIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJkb2QxTkZORE0zZDI1RVJURndkRE5ZTm1oV1JGVlJlbE5xYzBoNmFXNXdXRE5ZUm5aTmFsSkJiVGQ1TGxaNk5rMXJhR2d4WlRWRFJWbFpjVFpLUWxWalZGbzJRM0F5Y21GdVExZFNjblkzV1dGNE0weGxORTQxT1ZJMlpHUXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRsb1lrZHNhbHBUTld0aFYxRjFXbTB4Ym1ORE5XaGpTRUYyU1dsM2FXTnBTVFpYTVRCelNXMUZhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxFak5reFRaMmgzVTBVME16ZDNia1JGTVhCME0xZzJhRlpFVlZGNlUycHpTSHBwYm5CWU0xaEdkazFxVWtGdE4zayIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0\",\"recipients\":[{\"encrypted_key\":\"1ANpjMljSnNVJplZVU2II2F3TkVzNJ1Zu8Z0kSofZL3WiVxRcSU5ZIvwqpTEFqa2qEE1pqNO_50vPmdK_HYaNEyhhUk6VH2i\",\"header\":{\"kid\":\"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y\"}}],\"tag\":\"lBEJ_YmRqmPTxnq5fG4itle3lYgUekyJKj5yzWeCf7A\",\"iv\":\"tCELSamxTWMAzJ75PGJ17w\"}";
    let msg = "{
        \"ciphertext\" : \"xmLrSYBRs9A8oOub4zF0LaIBBRFnrpdGYRlZ2QeIpup_IOxnLY6vQkf7p7sQn3a6Vb_jD8cb0rCp5v5DnKE5IdXztlRY7HDFaJXOPWFsJLPMlvE0b5udm56ppCEr_dhcDN7pILwdOwjLi7QU1L4VXtRIGU-lK2eLmfdRbagAPOtx-2DQDVfDV6BQgXM1Gai7cgN2cBjQnjwLrasJ3zdah8QVFbc24mn1tNcpN-KtjSRtLHV0uDp8e7Vg4PZyyXgR4UhQm98SYdZa7Y3nLlLpzyz6_-X8z-jELkwdnYFtyuGRcBHIhR9oCKP9wOooexQ5xjd70E0n1D_frUOTTqyNPgWPH_zBy_cn3Lc15oF2JKH3Hannerk13CDHdpecB3Nn1xBZW9-nF6FHERZNIxItntw89x7fZiXsGaVBYkvCBvyeCg6MAoGcGG0ULCl726jK-XfEiH6Ay0Ma3IhoIJc1Sm_HwaXDVt1LlmaPpnP2uKMWzZ_Cp7cC28ik0a9vkrpDvAcf18bJPYu-fpkwFbxKg-SrSCmK6l25f0cMUbOXZoJaEPl89_4CRCeDMFM_fOvzbZ5XHlM9RyFHxlxuGzmILfw4qjP-OWGpx4Nw05XDoFun5NdQdcuTvnwYmXnTyva7SZ7S_XqhvduQdp6OazUB2O-A8CgUqiZYViReYN3ymUT1CzwWYY6Qu_CWalKNIHxktjYGyJJDNEFa_ivQS_kSGGACX8fYasO4iHMEhmoytfs7u3izlrl9t4YwUHeDMQxj\",
        \"protected\" : \"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6Im5ONnpDUk9BU1FpNzREWV9ld1VNM0dFQUMxRmZGSFdkd3ltWm5tTWo5V0kifSwiYXB2IjoiLWNOQ3l0eFVrSHpSRE5SckV2Vm05S0VmZzhZcUtQVnVVcVg1a0VLbU9yMCIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWhiR2xqWlM1a2FXUXVabTFuY0M1aGNIQXZJaXdpY2lJNlcxMHNJbUVpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmUSM2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eSIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJkb2QxTkZORE0zZDI1RVJURndkRE5ZTm1oV1JGVlJlbE5xYzBoNmFXNXdXRE5ZUm5aTmFsSkJiVGQ1TGxaNk5rMXJhR2d4WlRWRFJWbFpjVFpLUWxWalZGbzJRM0F5Y21GdVExZFNjblkzV1dGNE0weGxORTQxT1ZJMlpHUXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRsb1lrZHNhbHBUTld0aFYxRjFXbTB4Ym1ORE5XaGpTRUYyU1dsM2FXTnBTVFpYTVRCelNXMUZhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxFak5reFRaMmgzVTBVME16ZDNia1JGTVhCME0xZzJhRlpFVlZGNlUycHpTSHBwYm5CWU0xaEdkazFxVWtGdE4zayIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0\",
        \"recipients\" : [
          {
            \"encrypted_key\" : \"031m54woMUdatqTkxaMA1X9UsIn8wgww3bHzw6jPMHVOlk0i0vftSKw5kznLKdQbLCAXCZIr3Pxugqss3_YCDDOntHQ2Coiq\",
            \"header\" : {
              \"kid\" : \"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y\"
            }
          }
        ],
        \"tag\" : \"aV24qLYUe0Ulr6f2z-27ZjvFToGIaotMbNaC5sKIwkA\",
        \"iv\" : \"5BVjokm4Bj4CNw2heBr3oA\"
      }";

    // println!("Encryption metadata is\n{:?}\n", metadata);

    // --- Sending message by Alice ---
    println!("Alice is sending message \n{}\n", msg);

    // --- Unpacking message by Bob ---
    let did_resolver =
        ExampleDIDResolver::new(vec![BOB_DID_DOC.clone(), SCALA_ALICE_DID_DOC.clone()]);

    let secrets_resolver = ExampleSecretsResolver::new(SCALA_ALICE_SECRETS.clone());

    let (msg, metadata) = Message::unpack(
        &msg,
        &did_resolver,
        &secrets_resolver,
        &UnpackOptions::default(),
    )
    .await
    .expect("Unable unpack");

    println!("Received message is \n{:?}\n", msg);
    println!("Received message unpack metadata is \n{:?}\n", metadata);
}

// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

async fn bob_read_message() {
    // --- Building message from ALICE to BOB ---
    let msg = "{
        \"ciphertext\" : \"5n7qW8PLFpWKBJzsMrHm_wEEdGbR6JHOnHDUEt2c3vstFVuIvtf6zU42CYWBJV1dN8nCY4vlKxYltzr6delj-Yru3cJDcmllm3Jjb92aXNwFycBxvfl3KYzaz6e9wXake3Wc0Ez1ec73pkjd2IXk29zSAKCGROLVhGFb8wnAFJSiGp_LAfackBvqFwm8e9pRYAa_CFj4FVY5etsYBdOq6IuBIWblDiR33lcg3LJtVe2gM_9xWQU099d767uyafIm\",
        \"protected\" : \"eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTI1NiIsIngiOiJENmZkRUhreEd5N3BXZEE0YTdKbTRTSjRYRzQ3VFRycGxxclVlazNyaGN3IiwieSI6Inl1WDBwdmVidmNOcTdKUzJtMFh2dDhXSG83S3ZZTjRyTHc5SFZIbXpWTmsifSwiYXB2IjoiZzUxcksyYndwYkc1aXFtNmxrR01PaU4zbTBUSEllSzVzaDRqdld2N215QSIsInNraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktcDI1Ni0xIiwiYXB1IjoiWkdsa09tVjRZVzF3YkdVNllXeHBZMlVqYTJWNUxYQXlOVFl0TVEiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLWVuY3J5cHRlZCtqc29uIiwiZW5jIjoiQTI1NkNCQy1IUzUxMiIsImFsZyI6IkVDREgtMVBVK0EyNTZLVyJ9\",
        \"recipients\" : [
          {
            \"encrypted_key\" : \"JFMoOV0_KMlDiILu4DgoP2CU4Gd8ec5EgytMmflyAoNRcNtKYMpvldcKe5SxF5xegq1cZz9Fun4Y3MLOJOIELxMiIhQE1Yd8\",
            \"header\" : {
              \"kid\" : \"did:example:bob#key-p256-2\"
            }
          },
          {
            \"encrypted_key\" : \"2Awn8koIBMspu6NYaWWHW5T-QUi4SG6u9e9Lh_DMLh7AhiK6BLSNflA8TRpc__aGKkNCtV-MvKZDF65BQELIXIQTLim8Bgev\",
            \"header\" : {
              \"kid\" : \"did:example:bob#key-p256-1\"
            }
          },
          {
            \"encrypted_key\" : \"JFMoOV0_KMlDiILu4DgoP2CU4Gd8ec5EgytMmflyAoNRcNtKYMpvldcKe5SxF5xegq1cZz9Fun4Y3MLOJOIELxMiIhQE1Yd8\",
            \"header\" : {
              \"kid\" : \"did:example:bob#key-p256-not-secrets-1\"
            }
          }
        ],
        \"tag\" : \"dcwRTsE4lj-OEtjx3R8jQOLbBwK0MwZ3EdfnOvCKTfg\",
        \"iv\" : \"C17lyKMk-HuX4xv0-IHWDw\"
      }";

    // --- Unpacking message ---
    let did_resolver = ExampleDIDResolver::new(vec![ALICE_DID_DOC.clone(), BOB_DID_DOC.clone()]);

    let secrets_resolver = ExampleSecretsResolver::new(BOB_SECRETS.clone());

    let (msg, metadata) = Message::unpack(
        &msg,
        &did_resolver,
        &secrets_resolver,
        &UnpackOptions::default(),
    )
    .await
    .expect("Unable unpack");

    println!("Bob received message is \n{:?}\n", msg);
    println!("Bob received message unpack metadata is \n{:?}\n", metadata);
}

// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

async fn non_repudiable_encryption() {
    // --- Building message from ALICE to BOB ---
    let msg = Message::build(
        "example-1".to_owned(),
        "example/v1".to_owned(),
        json!({"aaa":"example-body"}),
    )
    .to(BOB_DID_DOC.id.to_owned())
    .from(ALICE_DID_DOC.id.to_owned())
    .finalize();

    // --- Packing encrypted and authenticated message ---
    let did_resolver = ExampleDIDResolver::new(vec![
        ALICE_DID_DOC.clone(),
        BOB_DID_DOC.clone(),
        // MEDIATOR1_DID_DOC.clone(),
    ]);

    let secrets_resolver = ExampleSecretsResolver::new(ALICE_SECRETS.clone());

    let (msg, metadata) = msg
        .pack_encrypted(
            &BOB_DID_DOC.id,
            Some(&ALICE_DID_DOC.id),
            Some(&ALICE_DID_DOC.id),
            &did_resolver,
            &secrets_resolver,
            //&PackEncryptedOptions::default(),
            &PackEncryptedOptions {
                protect_sender: false,
                forward: false,
                forward_headers: None,
                messaging_service: None,
                enc_alg_auth: AuthCryptAlg::default(),
                enc_alg_anon: AnonCryptAlg::default(),
            },
        )
        .await
        .expect("Unable pack_encrypted");

    println!("Encryption metadata is\n{:?}\n", metadata);

    // --- Sending message by Alice ---
    println!("Alice is sending message \n{}\n", msg);

    // // --- Unpacking message by Mediator1 ---
    // let did_resolver = ExampleDIDResolver::new(vec![
    //     ALICE_DID_DOC.clone(),
    //     BOB_DID_DOC.clone(),
    //     MEDIATOR1_DID_DOC.clone(),
    // ]);

    // let secrets_resolver = ExampleSecretsResolver::new(MEDIATOR1_SECRETS.clone());

    // let (msg, metadata) = Message::unpack(
    //     &msg,
    //     &did_resolver,
    //     &secrets_resolver,
    //     &UnpackOptions::default(),
    // )
    // .await
    // .expect("Unable unpack");

    // println!("Mediator1 received message is \n{:?}\n", msg);

    // println!(
    //     "Mediator1 received message unpack metadata is \n{:?}\n",
    //     metadata
    // );

    // // --- Forwarding message by Mediator1 ---
    // let msg = serde_json::to_string(&try_parse_forward(&msg).unwrap().forwarded_msg).unwrap();

    // println!("Mediator1 is forwarding message \n{}\n", msg);

    // --- Unpacking message ---
    let did_resolver = ExampleDIDResolver::new(vec![ALICE_DID_DOC.clone(), BOB_DID_DOC.clone()]);

    let secrets_resolver = ExampleSecretsResolver::new(BOB_SECRETS.clone());

    let (msg, metadata) = Message::unpack(
        &msg,
        &did_resolver,
        &secrets_resolver,
        &UnpackOptions::default(),
    )
    .await
    .expect("Unable unpack");

    println!("Bob received message is \n{:?}\n", msg);
    println!("Bob received message unpack metadata is \n{:?}\n", metadata);
}
