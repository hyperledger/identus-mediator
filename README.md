# SCALA DID

Implementation of data model from

- Decentralized Identifiers (DIDs) v1.0 - W3C Proposed Recommendation 03 August 2021 [LINK](https://w3c.github.io/did-core/)

Is a Scala/ScalaJS library for DID

## build and run app (open chrome)

`sbt>` `webapp/fastOptJS::webpack`

open `file:///home/fabio/workspace/ScalaDID/webapp/index-fastopt.html#/`

google-chrome-stable --disable-web-security --user-data-dir="/tmp/chrome_tmp" --new-window file:///home/fabio/workspace/ScalaDID/webapp/index-fastopt.html#/

## WIP/TODO

<https://github.com/w3c/?q=did&type=all&language=&sort>=

DID Document -> authentication -> challenge-response protocol
DID Document -> assertionMethod -> Issuer key (for purposes of issuing a Verifiable Credential)
DID Document -> keyAgreement -> tablishing a secure communication channel with the recipient
DID Document -> capabilityInvocation -> Master key (for authorization to update the DID Document.)
DID Document -> capabilityDelegation -> ...

### DID Credentials

Credentials -> [Verifiable Credentials Data Model v1.1](https://www.w3.org/TR/vc-data-model/)

### DID COM

DID Comm -> <https://identity.foundation/didcomm-messaging/spec/>
<https://didcomm.org/search/?page=1>

## FIXME for node v17

Error `Error: error:0308010C:digital envelope routines::unsupported`
export NODE_OPTIONS=--openssl-legacy-provider

## WIP

<https://raw.githubusercontent.com/w3c-ccg/did.actor/master/alice/did.json>
raw.githubusercontent.com/w3c%2Dccg/did.actor/master/alice
did:web:raw.githubusercontent.com:w3c%2Dccg:did.actor:master:alice
did:web:raw.githubusercontent.com:w3c%2Dccg%2Fdid.actor%2Fmaster%2Falice
