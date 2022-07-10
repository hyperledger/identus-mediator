package fmgp.did

import zio.json._

// case class GenericDID(
//     `@context`: SetU[String] = "https://w3id.org/did/v1",
//     id: String,
//     authentication: Authentication = None
// ) extends DID
//     with JSONLD {

//   // https://github.com/fthomas/refined
//   val (namespace, specificId) = id.split(":", 3) match {
//     case Array("did", ns, sId) => (ns, sId)
//     case _                     => throw new java.lang.AssertionError(s"Fail to parse id: '$id'")
//   }
// }
// object GenericDID {
//   implicit val decoder: JsonDecoder[GenericDID] = DeriveJsonDecoder.gen[GenericDID]
//   implicit val encoder: JsonEncoder[GenericDID] = DeriveJsonEncoder.gen[GenericDID]
// }

// // val a: Int Refined Greater[5] = 10

@main def main() =
  println("Hello, world")
//   println(
//     GenericDID(
//       "https://w3id.org/did/v1",
//       "did:github:OR13",
//       Some(
//         Set(
//           VerificationMethodClass(
//             id = "did:example:123456789abcdefghi#keys-1",
//             `type` = "Ed25519VerificationKey2020",
//             controller = "did:example:123456789abcdefghi",
//             publicKeyMultibase = "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV"
//           )
//         )
//       )
//     ).toJsonPretty // toJson
//   )

//   val aux = EX1.fromJson[GenericDID]

//   println(aux)
