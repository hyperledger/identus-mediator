package fmgp.did

import zio.json.ast.Json

package object comm {

  type UTCEpoch = Long
  type JSON_RFC7159 = Json // FIXME
  type Payload = String // Base64URL // TODO REMOVE

}
