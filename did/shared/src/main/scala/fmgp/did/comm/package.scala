package fmgp.did

import zio.json.ast.Json

package object comm {

  type UTCEpoch = Long
  type JSON_RFC7159 = Json.Obj
  object JSON_RFC7159 {
    def apply(fields: (String, Json)*): Json.Obj = Json.Obj(fields: _*)
  }

  type JWM_PROTECTED = String // TODO Base64 (URL)
  type JWM_SIGNATURE = String // TODO create a opaque for type safe

  type JWTToken = Json // TODO https://didcomm.org/book/v2/didrotation
}
