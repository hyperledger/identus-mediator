package fmgp.did.comm

/** https://identity.foundation/didcomm-messaging/spec/#message-headers */
enum Profiles(val name: String) {

  /** The encryption envelope, signing mechanism, plaintext conventions, and routing algorithms embodied in Aries AIP
    * 1.0, circa 2020.
    */
  case API1 extends Profiles("didcomm/aip1")

  /** The signing mechanism, plaintext conventions, and routing algorithms embodied in Aries AIP 2.0, circa 2021 — with
    * the old-style encryption envelope from Aries RFC 0019. This legal variant of AIP 2.0 minimizes differences with
    * codebases that shipped AIP 1.0 support.
    */
  case API2rfc19 extends Profiles("didcomm/aip2;env=rfc19")

  /** The signing mechanism, plaintext conventions, and routing algorithms embodied in Aries AIP 2.0, circa 2021 — with
    * the new-style encryption envelope from Aries RFC 0587. This legal variant of AIP 2.0 lays the foundation for
    * DIDComm v2 support by anticipating the eventual envelope change.
    */
  case API2rfc587 extends Profiles("didcomm/aip2;env=rfc587")

  /** The encryption envelope, signing mechanism, plaintext conventions, and routing algorithms embodied in this spec.
    */
  case V2 extends Profiles("didcomm/v2")
}
