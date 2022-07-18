package fmgp.crypto

import zio.json._

type Keys = Set[PrivateKey]
case class KeyStore(keys: Set[PrivateKey])

object KeyStore {
  given decoder: JsonDecoder[KeyStore] = // DeriveJsonDecoder.gen[Keys]
    JsonDecoder.set[PrivateKey].map(keys => KeyStore(keys))
  given encoder: JsonEncoder[KeyStore] = // DeriveJsonEncoder.gen[Keys]
    JsonEncoder.set[PrivateKey].contramap(ks => ks.keys)

}
