package fmgp.did

package object comm {

  type UTCEpoch = Long
  type JSON_RFC7159 = Map[String, String]

  type Base64URL = String // TODO
  type AuthenticationTag = Base64URL // TODO
  type InitializationVector = Base64URL // TODO

  type Base64URLHeaders = Base64URL // TODO
}
