# SCALA DID

A Scala/ScalaJS library for DID and DIDcomm

- Decentralized Identifiers (DIDs) v1.0 - W3C Proposed Recommendation 03 August 2021 [LINK GitHub](https://w3c.github.io/did-core/) or [LINK W3C](https://www.w3.org/TR/did-core/)
- DID Comm - <https://identity.foundation/didcomm-messaging/spec/>
- DID Comm protocols - <https://didcomm.org/search/?page=1>

## build and run app (open chrome)

`sbt>` `webapp/fastOptJS::webpack`

open `file:///home/fabio/workspace/ScalaDID/webapp/index-fastopt.html#/`

google-chrome-stable --disable-web-security --user-data-dir="/tmp/chrome_tmp" --new-window file:///home/fabio/workspace/ScalaDID/webapp/index-fastopt.html#/

## Limitations

### WIP

- I'm still working on encryption.
- Will define the out library API at the end.
  - The API will be asynchronous on both runtime environments (JavaScript; JVM), to be consistent.
  - Will have a ZIO types
- Define an interface for the DIDcomm DID resolver.

### Limitations in JS

ATM (as far as I know) no library was native JavaScript support for `ECHD-1PU` and `XC20P`.

[For an encrypted DIDComm message, the JWA of `ECDH-1PU` MUST be used within the structure of a JWE.](https://identity.foundation/didcomm-messaging/spec/#sender-authenticated-encryption)

The `XC20P` used for Anoncrypt but is optional.

You can read the [JavaScript JOSE Proposal](<https://hackmd.io/@IyhpRay4QVC_ozugDsQAQg/S1QlYJN0d>) from DIF (Decentralized Identity Foundation).
Also the discussion on the `jose` npm Library <https://github.com/panva/jose/discussions/237>

### Future Work

DID Credentials -> [Verifiable Credentials Data Model v1.1](https://www.w3.org/TR/vc-data-model/)

## Troubleshooting (Node v17)

Error `Error: error:0308010C:digital envelope routines::unsupported`
export NODE_OPTIONS=--openssl-legacy-provider
