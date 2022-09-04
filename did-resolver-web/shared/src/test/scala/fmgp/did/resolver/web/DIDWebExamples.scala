package fmgp.did.resolver.web

object DIDWebExamples {

  val ex4_did_to_url =
    "did:web:w3c-ccg.github.io" -> "https://w3c-ccg.github.io/.well-known/did.json"
  val ex5_did_with_path_to_url =
    "did:web:w3c-ccg.github.io:user:alice" -> "https://w3c-ccg.github.io/user/alice/did.json"
  val ex6_did_with_port_path_to_url =
    "did:web:example.com%3A3000:user:alice" -> "https://example.com:3000/user/alice/did.json"
}
