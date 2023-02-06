# Limitations

## Limitations in JS ATM

ATM no library has native JavaScript support for `ECHD-1PU` and `XC20P`.
- `ECHD-1PU` is used to create AUTHCRYPT message
- `XC20P` is optional and is used for content encryption of the message on ANONCRYPT

[For an encrypted DIDComm message, the JWA of `ECDH-1PU` MUST be used within the structure of a JWE.](https://identity.foundation/didcomm-messaging/spec/#sender-authenticated-encryption)

The `XC20P` used for Anoncrypt but is optional.

You can read the [JavaScript JOSE Proposal](<https://hackmd.io/@IyhpRay4QVC_ozugDsQAQg/S1QlYJN0d>) from DIF (Decentralized Identity Foundation).
Also the discussion on the `jose` npm Library <https://github.com/panva/jose/discussions/237>

## **Warning** about arbitrary JSON

- **Considerations about potential attacks on arbitrary JSON**

ATM the field `attachments` in `PlaintextMessage` is arbitrary JSON.
Like almost all json decoder libraries is easy for the final user to introduce potential well-known DOS attacks vectors.

**Solution one**

We plan to make a parametric `[T]` version of `PlaintextMessage` where the `attachments` is a type `T` (defined by the user).
On this version, the user needs to provide a `given` that encoded that type at compile time!

We will provide a macro that generates this decoder at compile time for `T`. Based on all DID Comm message types the user wants to support.

`T` is the union type of all DID Comm message types supported by the user.
The specialized decoder of a message will be chosen by the field `type` of the message.


With the macro provided by `zio.json`. The user can automatically generate highly optimized encoders and decoders at compile type. For each DID Comm message types.


**Solution two**

This solution will be an addition with what we have.

We will have a method on `PlaintextMessage`, where the attachments data will be bytes/string.

So the user can do whatever he wants. _It still has the same vulnerabilities considerations as what we have, but is no longer our responsibility_
