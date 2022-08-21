package fmgp

import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHEncrypterMulti
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.crypto.X25519EncrypterMulti

import com.nimbusds.jose.crypto.ECDH1PUDecrypter
import com.nimbusds.jose.crypto.ECDH1PUEncrypterMulti
import com.nimbusds.jose.crypto.ECDH1PUX25519Decrypter
import com.nimbusds.jose.crypto.ECDH1PUX25519EncrypterMulti
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.StandardCharset
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.X25519Encrypter
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.crypto.ECDH1PUEncrypter
import com.nimbusds.jose.crypto.ECDH1PUX25519Encrypter
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWEEncrypter

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm.EncryptedMessageGeneric
import fmgp.did.comm._
import zio.json._

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._
import scala.collection.JavaConverters._
import com.nimbusds.jose.UnprotectedHeader
import com.nimbusds.jose.util.Pair

package object crypto {

  type Base64URLString = String

  extension (alg: JWAAlgorithm) {
    def toJWSAlgorithm = alg match {
      case JWAAlgorithm.ES256K => JWSAlgorithm.ES256K
      case JWAAlgorithm.ES256  => JWSAlgorithm.ES256
      case JWAAlgorithm.ES384  => JWSAlgorithm.ES384
      case JWAAlgorithm.ES512  => JWSAlgorithm.ES512
      case JWAAlgorithm.EdDSA  => JWSAlgorithm.EdDSA
    }
  }
  extension (curve: Curve) {
    def toJWKCurve = curve match {
      case Curve.`P-256`   => JWKCurve.P_256
      case Curve.`P-384`   => JWKCurve.P_384
      case Curve.`P-521`   => JWKCurve.P_521
      case Curve.secp256k1 => JWKCurve.SECP256K1
      case Curve.X25519    => JWKCurve.X25519
      case Curve.Ed25519   => JWKCurve.Ed25519
    }
  }

  extension (ecKey: JWKECKey) {
    def verify(jwm: SignedMessage, alg: JWAAlgorithm): Boolean = {
      val _key = ecKey.toPublicJWK
      val verifier = new ECDSAVerifier(_key.toPublicJWK);
      val haeder = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(_key.getKeyID()).build()
      verifier.verify(
        haeder,
        (jwm.signatures.head.`protected` + "." + jwm.payload).getBytes(StandardCharset.UTF_8),
        Base64URL(jwm.signatures.head.signature) // FIXME .head
      )
    }

    def sign(plaintext: PlaintextMessageClass, alg: JWAAlgorithm): SignedMessage = { // TODO use PlaintextMessage
      require(ecKey.isPrivate(), "EC JWK must include the private key (d)")

      val signer: JWSSigner = new ECDSASigner(ecKey) // Create the EC signer
      val haeder: JWSHeader = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(ecKey.getKeyID()).build()
      val payloadObj = new Payload(plaintext.toJson)
      val jwsObject: JWSObject = new JWSObject(haeder, payloadObj) // Creates the JWS object with payload

      jwsObject.sign(signer)
      jwsObject.serialize().split('.') match {
        case Array(protectedValue, payload, signature) =>
          assert(payload == payloadObj.toBase64URL.toString) // redundant check
          assert(signature == jwsObject.getSignature.toString) // redundant check
          SignedMessage(
            payload = payload,
            Seq(JWMSignatureObj(`protected` = `protectedValue`, signature = signature)) // TODO haeder
          )
      }
    }
  }

  extension (okpKey: OctetKeyPair) {
    def verify(jwm: SignedMessage, alg: JWAAlgorithm): Boolean = {
      val _key = okpKey.toPublicJWK
      val verifier = new Ed25519Verifier(_key.toPublicJWK);
      val haeder = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(_key.getKeyID()).build()
      verifier.verify(
        haeder,
        (jwm.signatures.head.`protected` + "." + jwm.payload).getBytes(StandardCharset.UTF_8),
        Base64URL(jwm.signatures.head.signature) // FIXME .head
      )
    }

    def sign(plaintext: PlaintextMessageClass, alg: JWAAlgorithm): SignedMessage = { // TODO use PlaintextMessage
      require(okpKey.isPrivate(), "EC JWK must include the private key (d)")

      val signer: JWSSigner = new Ed25519Signer(okpKey) // Create the OKP signer
      val haeder: JWSHeader = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(okpKey.getKeyID()).build()
      val payloadObj = new Payload(plaintext.toJson)

      val jwsObject: JWSObject = new JWSObject(haeder, payloadObj) // Creates the JWS object with payload

      jwsObject.sign(signer)
      jwsObject.serialize().split('.') match {
        case Array(protectedValue, payload, signature) =>
          assert(payload == payloadObj.toBase64URL.toString) // redundant check
          assert(signature == jwsObject.getSignature.toString) // redundant check
          SignedMessage(
            payload = payload,
            Seq(JWMSignatureObj(`protected` = `protectedValue`, signature = signature)) // TODO haeder
          )
      }
    }
  }

  extension (key: OKP_EC_Key) {
    def toJWK: JWKECKey | OctetKeyPair = {
      key match {
        case ec: ECKey   => ec.toJWK
        case okp: OKPKey => okp.toJWK
      }
    }
  }

  extension (ec: ECKey) {
    def toJWK: JWKECKey = {

      val builder = ec.getCurve match {
        case c: Curve.`P-256`.type   => JWKECKey.Builder(c.toJWKCurve, Base64URL(ec.x), Base64URL(ec.y))
        case c: Curve.`P-384`.type   => JWKECKey.Builder(c.toJWKCurve, Base64URL(ec.x), Base64URL(ec.y))
        case c: Curve.`P-521`.type   => JWKECKey.Builder(c.toJWKCurve, Base64URL(ec.x), Base64URL(ec.y))
        case c: Curve.secp256k1.type => JWKECKey.Builder(c.toJWKCurve, Base64URL(ec.x), Base64URL(ec.y))
      }
      ec.kid.foreach(builder.keyID)
      ec match { // for private key
        case _: PublicKey  => // ok (just the public key)
        case k: PrivateKey => builder.d(Base64URL(k.d))
      }
      builder.build()
    }
  }
  extension (okp: OKPKey) {
    def toJWK: OctetKeyPair = {
      val builder = okp.getCurve match {
        case c: Curve.Ed25519.type => OctetKeyPair.Builder(c.toJWKCurve, Base64URL(okp.x))
        case c: Curve.X25519.type  => OctetKeyPair.Builder(c.toJWKCurve, Base64URL(okp.x))
      }
      okp.kid.foreach(builder.keyID)
      okp match { // for private key
        case _: PublicKey  => // ok (just the public key)
        case k: PrivateKey => builder.d(Base64URL(k.d))
      }
      builder.build()
    }
  }

  extension (key: PrivateKey) {
    def verify(jwm: SignedMessage): Future[Boolean] = Future.successful(
      key.toJWK match {
        case ecKey: JWKECKey      => ecKey.verify(jwm, key.jwaAlgorithmtoSign)
        case okpKey: OctetKeyPair => okpKey.verify(jwm, key.jwaAlgorithmtoSign)
      }
    )

  }

  extension (key: OKP_EC_Key) {
    def sign(plaintext: PlaintextMessageClass): Future[SignedMessage] =
      Future.successful( // TODO use PlaintextMessageClass
        key.toJWK match {
          case ecKey: JWKECKey      => ecKey.sign(plaintext, key.jwaAlgorithmtoSign)
          case okpKey: OctetKeyPair => okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        }
      )
  }

  // ###############
  // ### encrypt ###
  // ###############
  // TODO https://identity.foundation/didcomm-messaging/spec/#key-wrapping-algorithms
  // REMOVE https://bitbucket.org/connect2id/nimbus-jose-jwt/branch/release-9.16?dest=release-9.16-preview.1

  def encrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): EncryptedMessageGeneric = anoncrypt(recipientKidsKeys, data: Array[Byte])

  def encrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): EncryptedMessageGeneric = authcrypt(senderKidKey, recipientKidsKeys, data)

  /** anoncrypt - Guarantees confidentiality and integrity without revealing the identity of the sender.
    */
  def anoncrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): EncryptedMessageGeneric =
    recipientKidsKeys
      .foldRight(
        (Seq.empty[(VerificationMethodReferenced, JWKECKey)], Seq.empty[(VerificationMethodReferenced, OctetKeyPair)])
      ) { (e, acu) =>
        e._2.toJWK match
          case ecKey: JWKECKey      => (acu._1 :+ (e._1, ecKey), acu._2)
          case okpKey: OctetKeyPair => (acu._1, acu._2 :+ (e._1, okpKey))
      }
      .pipe(e => (e._1.sortBy(_._1.value), e._2.sortBy(_._1.value))) // order recipients by name
      .pipe {
        case (Seq(), Seq())    => ???
        case (ecKeys, Seq())   => anoncryptEC(ecKeys, data)
        case (Seq(), okpKeys)  => anoncryptOKP(okpKeys, data)
        case (ecKeys, okpKeys) => ???
      }

  /** authcrypt - Guarantees confidentiality and integrity. Also proves the identity of the sender – but in a way that
    * only the recipient can verify. This is the default wrapping choice, and SHOULD be used unless a different goal is
    * clearly identified. By design, this combination and all other combinations that use encryption in their outermost
    * layer share an identical IANA media type, because only the recipient should care about the difference.
    */
  def authcrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): EncryptedMessageGeneric =
    (senderKidKey._2).toJWK match {
      case (ecSenderKey: JWKECKey) =>
        val recipientKeys = recipientKidsKeys.map {
          case (vmr, key: ECPublicKey)  => (vmr, key.toJWK)
          case (vmr, key: OKPPublicKey) => ??? // FIXME
        }
        authcryptEC((senderKidKey._1, ecSenderKey), recipientKeys, data)
      case okpSenderKey: OctetKeyPair =>
        val recipientKeys = recipientKidsKeys.map {
          case (vmr, key: ECPublicKey)  => ??? // FIXME
          case (vmr, key: OKPPublicKey) => (vmr, key.toJWK)
        }
        authcryptOKP((senderKidKey._1, okpSenderKey), recipientKeys, data)
    }

  // ###############
  // ### Methods ###
  // ###############

  def anoncryptEC( // anoncryptECDH
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, JWKECKey)],
      clearText: Array[Byte]
  ) = {
    val aux = ecRecipientsKeys.map(e =>
      Pair.of(
        UnprotectedHeader.Builder().keyID(e._1.value).build(),
        e._2
      )
    )
    val encrypter = ECDHEncrypterMulti(aux.asJava)

    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512) // XC20P)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .agreementPartyVInfo(Utils.calculateAPV(ecRecipientsKeys.map(_._1)))
      .build()
    val parts = encrypter.encrypt(header, clearText)
    val recipients = parts.getRecipients.asScala.toSeq.map { e =>
      Recipient(e.getEncryptedKey().toString(), RecipientHeader(VerificationMethodReferenced(e.getHeader().getKeyID())))
    }

    EncryptedMessageGeneric(
      ciphertext = parts.getCipherText().toString, // : Base64URL,
      `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
      recipients = recipients, // auxRecipient.toSeq,
      tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
      iv = parts.getInitializationVector().toString // : InitializationVector
    )
  }

  def anoncryptOKP( // anoncryptECDH_X25519
      okpRecipientKeys: Seq[(VerificationMethodReferenced, OctetKeyPair)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {
    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .agreementPartyVInfo(Utils.calculateAPV(okpRecipientKeys.map(_._1)))
      .build()
    MyX25519Encrypter(okpRecipientKeys, header).encrypt(clearText)
  }

  def authcryptEC( // encrypterECDH1PU
      senderKidKey: (VerificationMethodReferenced, JWKECKey),
      recipientKeys: Seq[(VerificationMethodReferenced, JWKECKey)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {
    val aux = recipientKeys.map(e =>
      Pair.of(
        UnprotectedHeader.Builder().keyID(e._1.value).build(),
        e._2
      )
    )
    val encrypter = ECDH1PUEncrypterMulti(senderKidKey._2, aux.asJava)

    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_1PU_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .senderKeyID(senderKidKey._1.value)
      .agreementPartyUInfo(Utils.calculateAPU(senderKidKey._1))
      .agreementPartyVInfo(Utils.calculateAPV(recipientKeys.map(_._1)))
      .build()

    val parts = encrypter.encrypt(header, clearText)
    val recipients = parts.getRecipients.asScala.toSeq
      .map { e =>
        Recipient(
          e.getEncryptedKey().toString(),
          RecipientHeader(VerificationMethodReferenced(e.getHeader().getKeyID()))
        )
      }

    EncryptedMessageGeneric(
      ciphertext = parts.getCipherText().toString, // : Base64URL,
      `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
      recipients = recipients, // auxRecipient.toSeq,
      tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
      iv = parts.getInitializationVector().toString // : InitializationVector
    )

  }

  def authcryptOKP( // encrypterECDH1PU_X25519
      senderKidKey: (VerificationMethodReferenced, OctetKeyPair),
      recipientKeys: Seq[(VerificationMethodReferenced, OctetKeyPair)],
      clearText: Array[Byte]
  ): EncryptedMessageGeneric = {
    val aux = recipientKeys.map(e =>
      Pair.of(
        UnprotectedHeader.Builder().keyID(e._1.value).build(),
        e._2
      )
    )

    val encrypter = ECDH1PUX25519EncrypterMulti(senderKidKey._2, aux.asJava)

    val header: JWEHeader = new JWEHeader.Builder(JWEAlgorithm.ECDH_1PU_A256KW, EncryptionMethod.A256CBC_HS512)
      .`type`(JOSEObjectType("application/didcomm-encrypted+json"))
      .senderKeyID(senderKidKey._1.value)
      .agreementPartyUInfo(Utils.calculateAPU(senderKidKey._1))
      .agreementPartyVInfo(Utils.calculateAPV(recipientKeys.map(_._1)))
      .build()
    val parts = encrypter.encrypt(header, clearText)
    val recipients = parts.getRecipients.asScala.toSeq
      .map { e =>
        Recipient(
          e.getEncryptedKey().toString(),
          RecipientHeader(VerificationMethodReferenced(e.getHeader().getKeyID()))
        )
      }

    EncryptedMessageGeneric(
      ciphertext = parts.getCipherText().toString, // : Base64URL,
      `protected` = Base64URL.encode(parts.getHeader().toString).toString(), // : Base64URLHeaders,
      recipients = recipients, // auxRecipient.toSeq,
      tag = parts.getAuthenticationTag().toString, // AuthenticationTag,
      iv = parts.getInitializationVector().toString // : InitializationVector
    )
  }

  // ###############
  // ### decrypt ###
  // ###############

  def decrypt(
      key: PrivateKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): Future[String] = Future.successful {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`))
    key.toJWK match {
      case ecKey: JWKECKey =>
        val ret = ECDHDecrypter(ecKey).decrypt(
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
      case okpKey: OctetKeyPair => // okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        val ret = X25519Decrypter(okpKey).decrypt(
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
    }
  }

  def decryptAndVerify(
      keyDecrypt: PrivateKey,
      keyToVerify: PublicKey,
      encryptedKey: String,
      msg: EncryptedMessageGeneric
  ): Future[String] = Future.successful {
    val header: JWEHeader = JWEHeader.parse(Base64URL(msg.`protected`))
    (keyDecrypt.toJWK, keyToVerify.toJWK) match {
      case (ecKey: JWKECKey, ecKeyToVerify: JWKECKey) =>
        val ret = ECDH1PUDecrypter(ecKey.toECPrivateKey(), ecKeyToVerify.toECPublicKey()).decrypt( // FIXME
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
      case (okpKey: OctetKeyPair, okpKeyToVerify: OctetKeyPair) => // okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
        val ret = ECDH1PUX25519Decrypter(okpKey, okpKeyToVerify) decrypt (
          header,
          Base64URL(encryptedKey),
          Base64URL(msg.iv),
          Base64URL(msg.ciphertext),
          Base64URL(msg.tag)
        )
        String(ret)
      case _ => throw new AssertionError("The Keys must be of the same type!") // FIXME Make function type safe!
    }
  }

}
