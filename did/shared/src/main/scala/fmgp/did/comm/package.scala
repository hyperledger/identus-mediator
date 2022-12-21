package fmgp.did

import zio.json.ast.Json

package object comm {

  type UTCEpoch = Long
  type JSON_RFC7159 = Json
  type Payload = String // Base64URL // TODO REMOVE
  type JWM_PROTECTED = String // TODO Base64 (URL)
  type JWM_SIGNATURE = String // TODO create a opaque for type safe

}
