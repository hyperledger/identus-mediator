package fmgp.did

import fmgp.crypto._
import zio.json.ast.Json

object DIDExamples {

  /*EXAMPLE1: A simple DID document - https://www.w3.org/TR/did-core/#a-simple-example*/
  val EX1 = """
{
  "@context": [
    "https://www.w3.org/ns/did/v1",
    "https://w3id.org/security/suites/ed25519-2020/v1"
  ],
  "id": "did:example:123456789abcdefghi",
  "authentication": [{
    "id": "did:example:123456789abcdefghi#keys-1",
    "type": "Ed25519VerificationKey2020",
    "controller": "did:example:123456789abcdefghi",
    "publicKeyMultibase": "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV"
  }]
}
""".stripMargin

  /* EXAMPLE 2 - https://w3c.github.io/did-core/#path */
  val EX2 = """did:example:123456/path"""

  /* EXAMPLE 3 - https://w3c.github.io/did-core/#query */
  val EX3 = """did:example:123456?versionId=1"""

  /* EXAMPLE 4: A unique verification method in a DID Document - https://w3c.github.io/did-core/#fragment */
  val EX4 = """did:example:123456?versionId=1"""
  /* EXAMPLE 5: A unique service in a DID Document - https://w3c.github.io/did-core/#fragment */
  val EX5 = """did:example:123#agent"""
  /* EXAMPLE 6: A resource external to a DID Document - https://w3c.github.io/did-core/#fragment */
  val EX6 = """did:example:123?service=agent&relativeRef=/credentials#degree"""

  /* EXAMPLE 7: A DID URL with a 'versionTime' DID parameter - https://w3c.github.io/did-core/#did-parameters */
  val EX7 = """did:example:123?versionTime=2021-05-10T17:00:00Z"""
  /* EXAMPLE 8: A DID URL with a 'service' and a 'relativeRef' DID parameter - https://w3c.github.io/did-core/#did-parameters */
  val EX8 = """did:example:123?service=files&relativeRef=/resume.pdf"""

  /* EXAMPLE 9: An example of a relative DID URL - https://w3c.github.io/did-core/#relative-did-urls */
  val EX9 = """
{
  "@context": [
    "https://www.w3.org/ns/did/v1",
    "https://w3id.org/security/suites/ed25519-2020/v1"
  ]
  "id": "did:example:123456789abcdefghi",
  "verificationMethod": [{
    "id": "did:example:123456789abcdefghi#key-1",
    "type": "Ed25519VerificationKey2020", 
    "controller": "did:example:123456789abcdefghi",
    "publicKeyMultibase": "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV"
  }, ...],
  "authentication": [

    "#key-1"
  ]
}
""".stripMargin

  /* EXAMPLE 10 -https://w3c.github.io/did-core/#did-subject */
  val EX10 = """{"id": "did:example:123456789abcdefghijk"}"""
  val EX10_DIDDocument = DIDDocumentClass(id = DIDSubject("did:example:123456789abcdefghijk"))

  /* EXAMPLE 11: DID document with a controller property - https://w3c.github.io/did-core/#did-controller */
  val EX11 = """
{
  "@context": "https://www.w3.org/ns/did/v1",
  "id": "did:example:123456789abcdefghi",
  "controller": "did:example:bcehfew7h32f32h7af3",
}
""".stripMargin

  /* EXAMPLE 12: Example verification method structure - https://w3c.github.io/did-core/#verification-methods */
  val EX12 = """
{
  "@context": [
    "https://www.w3.org/ns/did/v1",
    "https://w3id.org/security/suites/jws-2020/v1"
    "https://w3id.org/security/suites/ed25519-2020/v1"
  ]
  "id": "did:example:123456789abcdefghi",
  ...
  "verificationMethod": [{
    "id": ...,
    "type": ...,
    "controller": ...,
    "publicKeyJwk": ...
  }, {
    "id": ...,
    "type": ...,
    "controller": ...,
    "publicKeyMultibase": ...
  }]
}
""".stripMargin

  /* EXAMPLE 13: Verification methods using publicKeyJwk and publicKeyMultibase - https://w3c.github.io/did-core/#verification-material */
  val EX13 = """
{
  "@context": [
    "https://www.w3.org/ns/did/v1",
    "https://w3id.org/security/suites/jws-2020/v1",
    "https://w3id.org/security/suites/ed25519-2020/v1"
  ],
  "id": "did:example:123456789abcdefghi",
  "verificationMethod": [{
    "id": "did:example:123#_Qq0UL2Fq651Q0Fjd6TvnYE-faHiOpRlPVQcY_-tA4A",
    "type": "JsonWebKey2020", 
    "controller": "did:example:123",
    "publicKeyJwk": {
      "crv": "Ed25519", 
      "x": "VCpo2LMLhn6iWku8MKvSLg2ZAoC-nlOyPVQaO3FxVeQ", 
      "kty": "OKP", 
      "kid": "_Qq0UL2Fq651Q0Fjd6TvnYE-faHiOpRlPVQcY_-tA4A" 
    }
  }, {
    "id": "did:example:123456789abcdefghi#keys-1",
    "type": "Ed25519VerificationKey2020", 
    "controller": "did:example:pqrstuvwxyz0987654321",
    "publicKeyMultibase": "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV"
  }]
}
""".stripMargin

  val EX13_VerificationMethod_0 = VerificationMethodEmbeddedJWK(
    id = "did:example:123#_Qq0UL2Fq651Q0Fjd6TvnYE-faHiOpRlPVQcY_-tA4A",
    controller = "did:example:123",
    `type` = "JsonWebKey2020",
    publicKeyJwk = OKPPublicKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      x = "VCpo2LMLhn6iWku8MKvSLg2ZAoC-nlOyPVQaO3FxVeQ",
      kid = Some("_Qq0UL2Fq651Q0Fjd6TvnYE-faHiOpRlPVQcY_-tA4A")
    )
  )

  val EX13_VerificationMethod_1 = VerificationMethodEmbeddedMultibase(
    id = "did:example:123456789abcdefghi#keys-1",
    controller = "did:example:pqrstuvwxyz0987654321",
    `type` = "Ed25519VerificationKey2020",
    publicKeyMultibase = "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV",
  )

  /* EXAMPLE 14: Embedding and referencing verification methods - https://w3c.github.io/did-core/#referring-to-verification-methods */
  val EX14 = """
{
  "authentication": [    
    "did:example:123456789abcdefghi#keys-1",
    {
      "id": "did:example:123456789abcdefghi#keys-2",
      "type": "Ed25519VerificationKey2020", 
      "controller": "did:example:123456789abcdefghi",
      "publicKeyMultibase": "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV"
    }
  ]
}
""".stripMargin

  /** EXAMPLE 17: Key agreement property containing two verification methods -
    * https://www.w3.org/TR/did-core/#key-agreement
    */
  val EX17 = """
{
  "@context": "https://www.w3.org/ns/did/v1",
  "id": "did:example:123456789abcdefghi",
  "keyAgreement": [
    "did:example:123456789abcdefghi#keys-1",
    {
      "id": "did:example:123#zC9ByQ8aJs8vrNXyDhPHHNNMSHPcaSgNpjjsBYpMMjsTdS",
      "type": "X25519KeyAgreementKey2019",
      "controller": "did:example:123",
      "publicKeyMultibase": "z9hFgmPVfmBZwRvFEyniQDBkz9LmV7gDEqytWyGZLmDXE"
    }
  ]
}
""".stripMargin

  val EX17_DIDDocument = DIDDocumentClass(
    id = DIDSubject("did:example:123456789abcdefghi"),
    keyAgreement = Some(
      Set(
        VerificationMethodReferenced("did:example:123456789abcdefghi#keys-1"),
        VerificationMethodEmbeddedMultibase(
          id = "did:example:123#zC9ByQ8aJs8vrNXyDhPHHNNMSHPcaSgNpjjsBYpMMjsTdS",
          controller = "did:example:123",
          `type` = "X25519KeyAgreementKey2019",
          publicKeyMultibase = "z9hFgmPVfmBZwRvFEyniQDBkz9LmV7gDEqytWyGZLmDXE",
        )
      )
    )
  )

  /* EXAMPLE 20: Usage of the service property - https://w3c.github.io/did-core/#services */
  val EX20 = """
{
  "service": [{
    "id":"did:example:123#linked-domain",
    "type": "LinkedDomains", 
    "serviceEndpoint": "https://bar.example.com"
  }]
}
""".stripMargin
  val EX20_DIDService = DIDServiceGeneric(
    id = "did:example:123#linked-domain",
    `type` = "LinkedDomains",
    serviceEndpoint = Json.Str("https://bar.example.com")
  )

  val EX39 = """
{
  "@context": "https://w3id.org/security/v1",
  "@id": "https://payswarm.example.com/i/bob/keys/1",
  "@type": "Key",
  "owner": "https://payswarm.example.com/i/bob",
  "publicKeyPem": "-----BEGIN PUBLIC KEY-----\nMII8YbF3s8q3c...j8Fk88FsRa3K\n-----END PUBLIC KEY-----\n"
}
""".stripMargin

// https://github.com/OR13/ghdid/blob/master/index.jsonld
  val test = """
{
  "@context": "https://w3id.org/did/v1",
  "id": "did:github:OR13",
  "publicKey": [
    {
      "type": "OpenPgpVerificationKey2019",
      "id": "did:github:OR13#R-s6_TXT1tv2TxinOE9puChzik_cIfqSQXoDlc7TTL8",
      "controller": "did:github:OR13",
      "publicKeyPem": "-----BEGIN PGP PUBLIC KEY BLOCK-----\r\nVersion: OpenPGP.js v4.6.2\r\nComment: https://openpgpjs.org\r\n\r\nxk8EXdAW1xMFK4EEAAoCAwTsedWnNnkIPRwywHtdXL4L6BkmNHNR9cCtB6gx\r\nuunRiiwBnowDSb1Ywn6P9iKBRQmB1f0d7+Y3Ps7c2VMxdbh/zRdhbm9uIDxh\r\nbm9uQGV4YW1wbGUuY29tPsJ3BBATCAAfBQJd0BbXBgsJBwgDAgQVCAoCAxYC\r\nAQIZAQIbAwIeAQAKCRBrQmWOSjzAbkEbAP0WLFrXdxasz5zB5BgAkshEDlLV\r\nJxrAGINx9pz8flQmowEAvquv0oDKZYxjO2eRtUQ717WIp5qfSsAhL+wmuqie\r\n+jTOUwRd0BbXEgUrgQQACgIDBGvprothosSrLPLNDHndL9XAN2yBoCFss4nL\r\nFC+Wzi2/kJSq1AR1BfvkESpEvXk+UIztFrI24JX22s8tpVBOoVADAQgHwmEE\r\nGBMIAAkFAl3QFtcCGwwACgkQa0Jljko8wG5P4QD+KiJiuXIwjERVXGMuUtf7\r\n9rAqOLe/s4Nn+o5IviQGCgcA/1FxvLz+LZCtZ+uzoCh07v1z0dZizn6RMWRR\r\nHAgQXD7k\r\n=VYgR\r\n-----END PGP PUBLIC KEY BLOCK-----\r\n"
    },
    {
      "type": "OpenPgpVerificationKey2019",
      "id": "did:github:OR13#O8XyI6_2GE5dEBjHCYpM4KZZAJUnV2gKAGG6pGfnls0",
      "controller": "did:github:OR13",
      "publicKeyPem": "-----BEGIN PGP PUBLIC KEY BLOCK-----\r\nVersion: OpenPGP.js v4.6.2\r\nComment: https://openpgpjs.org\r\n\r\nxk8EXdAW1xMFK4EEAAoCAwTfmBvbWKnGoxlPKEX/brPYTroIRu7U4aLigsJg\r\nPZbXaBECnF2nqR/5htMTxqtw/4I7ADorG+Q8Ugi2V+K1ARu5zRdhbm9uIDxh\r\nbm9uQGV4YW1wbGUuY29tPsJ3BBATCAAfBQJd0BbXBgsJBwgDAgQVCAoCAxYC\r\nAQIZAQIbAwIeAQAKCRDZ/gPbP1dkrNZ9AP9bY0x7x44VyM+fXYgqFGqXfce3\r\nCrPUPq8nZF6ki337AQD+IQ5M9dxWkAWjHcXsQxGSN3BnZN7QtYFV/wjrBQ7A\r\nBWrOUwRd0BbXEgUrgQQACgIDBNt00kWCAbi9YTx2JRo6kGWk9QzTHvwCr5Ix\r\nVTRBwIyHhd9NsQO1EZxrARXJDJ+rY2gsxL0o5Zg+6E6Zu84GD4YDAQgHwmEE\r\nGBMIAAkFAl3QFtcCGwwACgkQ2f4D2z9XZKxxLQEAueTzlPrGpIr0L5SiB0xt\r\nTwypd4nK2dZZ2TtfzUvFeToBAPVYnUCUECTDdliOfhiRdOsWQRkb+ERRDBX3\r\n4rYhtMEV\r\n=M2JO\r\n-----END PGP PUBLIC KEY BLOCK-----\r\n"
    },
    {
      "type": "Ed25519VerificationKey2018",
      "id": "did:github:OR13#XJZ3sHC-idqckHQCYQSQBlNYcBNxh8FePiD_KenzZ2o",
      "controller": "did:github:OR13",
      "publicKeyBase58": "DNtjYfcgPmMM3FjjtrvUsn5WDJiEYKpg1qwVyqGdHkhf"
    }
  ],
  "authentication": [],
  "service": [],
  "capabilityDelegation": [
    "did:github:OR13#XJZ3sHC-idqckHQCYQSQBlNYcBNxh8FePiD_KenzZ2o"
  ],
  "capabilityInvocation": [
    "did:github:OR13#XJZ3sHC-idqckHQCYQSQBlNYcBNxh8FePiD_KenzZ2o"
  ],
  "assertionMethod": [
    "did:github:OR13#XJZ3sHC-idqckHQCYQSQBlNYcBNxh8FePiD_KenzZ2o"
  ],
  "keyAgreement": [
    {
      "id": "did:github:OR13#zC6JZRVUXwWkJGLRvvTtaJbxuNjq4x6Cs1VnPLCun5miSe",
      "type": "X25519KeyAgreementKey2019",
      "controller": "did:github:OR13",
      "publicKeyBase58": "6oqi8Hik911qENxU4Q1LSoE5VZFzXWFWbu6JMpygfULS"
    }
  ],
  "proof": {
    "type": "Ed25519Signature2018",
    "created": "2019-11-16T15:33:43Z",
    "verificationMethod": "did:github:OR13#XJZ3sHC-idqckHQCYQSQBlNYcBNxh8FePiD_KenzZ2o",
    "proofPurpose": "assertionMethod",
    "jws": "eyJhbGciOiJFZERTQSIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..6HTKO58kIVZ4WHDJSVBYhaa-cMWUnOdlS0h7N4F0WpdqhS_L0lTA3qtezKXEh8RU0UvVdA1yDz70wwN5UPIGDg"
  }
}
""".stripMargin

}
