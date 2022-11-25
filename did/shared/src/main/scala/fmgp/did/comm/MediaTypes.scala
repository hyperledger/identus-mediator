package fmgp.did.comm

import zio.json._

/** IANA Media Types
  *
  * https://identity.foundation/didcomm-messaging/spec/#iana-media-types
  */
enum MediaTypes(val typ: String) {

  /** Used as the building block of higher-level protocols, but rarely transmitted directly, since it lacks security
    * guarantees.
    */
  case PLAINTEXT extends MediaTypes("application/didcomm-plain+json") // plaintext(noEnvelope)

  /** Adds non-repudiation to a plaintext message; whoever receives a message wrapped in this way can prove its origin
    * to any external party.
    */
  case SIGNED extends MediaTypes("application/didcomm-signed+json") // signed(plaintext)

  /** ENCRYPTED is a generic just to be used by the [[MediaTypes.decoder]] */
  case ENCRYPTED extends MediaTypes("application/didcomm-encrypted+json")

  /** Guarantees confidentiality and integrity without revealing the identity of the sender. */
  case ANONCRYPT extends MediaTypes("application/didcomm-encrypted+json") // anoncrypt(plaintext)

  /** Guarantees confidentiality and integrity. Also proves the identity of the sender – but in a way that only the
    * recipient can verify. This is the default wrapping choice, and SHOULD be used unless a different goal is clearly
    * identified. By design, this combination and all other combinations that use encryption in their outermost layer
    * share an identical IANA media type, because only the recipient should care about the difference.
    */
  case AUTHCRYPT extends MediaTypes("application/didcomm-encrypted+json") // authcrypt(plaintext)

  /** Guarantees confidentiality, integrity, and non-repudiation – but prevents an observer of the outer envelope from
    * accessing the signature. Relative to authcrypt(plaintext), this increases guarantees to the recipient, since
    * non-repudiation is strong than simple authentication. However, it also forces the sender to talk “on the record”
    * and is thus not assumed to be desirable by default.
    */
  case ANONCRYPT_SIGN extends MediaTypes("application/didcomm-encrypted+json") // anoncrypt(sign(plaintext))

  /** Adds no useful guarantees over the previous choice, and is slightly more expensive, so this wrapping combination
    * SHOULD NOT be emitted by conforming implementations. However, implementations MAY accept it. If they choose to do
    * so, they MUST emit an error if the signer of the plaintext is different from the sender identified by the
    * authcrypt layer.
    */
  case AUTHCRYPT_SIGN extends MediaTypes("application/didcomm-encrypted+json") // authcrypt(sign(plaintext))

  /** A specialized combination that hides the skid header in the authcrypt envelope, so the hop immediately sourceward
    * of a mediator cannot discover an identifier for the sender. See Protecting the Sender Identity.
    */
  case ANONCRYPT_AUTHCRYPT extends MediaTypes("application/didcomm-encrypted+json") // anoncrypt(authcrypt(plaintext))

}

object MediaTypes {
  given decoder: JsonDecoder[MediaTypes] = JsonDecoder.string
    .map {
      case "application/didcomm-plain+json"     => PLAINTEXT
      case "application/didcomm-signed+json"    => SIGNED
      case "application/didcomm-encrypted+json" => ENCRYPTED
    }
  given encoder: JsonEncoder[MediaTypes] = JsonEncoder.string.contramap(e => e.typ)
}
