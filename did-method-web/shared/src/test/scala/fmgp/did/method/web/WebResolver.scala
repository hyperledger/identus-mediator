package fmgp.did.method.web

// DID Specification Registrie -> https://www.w3.org/TR/did-spec-registries/
// https://w3c-ccg.github.io/did-method-web/
// DEMO -> https://dev.uniresolver.io/

object WebResolver {
  val ex1 =
    "did:web:did.actor:alice" ->
      "https://did.actor/alice/did.json"
  val ex2 =
    "did:web:did.actor:bob" ->
      "https://did.actor/bob/did.json"
  val ex3 =
    "did:web:did.actor:carol" ->
      "https://did.actor/carol/did.json"
  val ex4 =
    "did:web:did.actor:mike" ->
      "https://did.actor/mike/did.json"
  val ex5 =
    "did:web:identity.foundation" ->
      "https://identity.foundation/.well-known/did.json"
  val ex6 =
    "did:web:raw.githubusercontent.com:FabioPinheiro:did.actor:master:alice" ->
      "https://raw.githubusercontent.com/FabioPinheiro/did.actor/master/alice/did.json"

}

// ??? https://identity.foundation/.well-known/did-configuration.json
