package fmgp.did.method.peer

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import org.didcommx.peerdid.core.PeerDIDUtils
import org.didcommx.peerdid.*

import com.nimbusds.jose.jwk.*
import scala.jdk.CollectionConverters.*

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.UtilsJVM._

import zio.json._
import fmgp.crypto.OKPPrivateKey

final case class DidcommxPeerdid(
    jwkForKeyAgreement: OKPKey = KeyGenerator.newEd25519.toOption.get,
    jwkForKeyAuthentication: OKPKey = KeyGenerator.newX25519.toOption.get,
    serviceEndpoint: Option[String] = None
) {

  def did = DIDSubject(
    org.didcommx.peerdid.PeerDIDCreator.createPeerDIDNumalgo2(
      List(keyAgreement).asJava,
      List(keyAuthentication).asJava,
      serviceEndpoint match {
        case Some(endpoint) => DIDPeerServiceEncoded(endpoint).toJson
        case None           => null
      }
    )
  )

  def keyAgreement = VerificationMaterialPeerDID[VerificationMethodTypeAgreement](
    VerificationMaterialFormatPeerDID.JWK,
    jwkForKeyAgreement.toJWK.toPublicJWK,
    VerificationMethodTypeAgreement.JSON_WEB_KEY_2020.INSTANCE
  )

  def keyAuthentication = VerificationMaterialPeerDID[VerificationMethodTypeAuthentication](
    VerificationMaterialFormatPeerDID.JWK,
    jwkForKeyAuthentication.toJWK.toPublicJWK,
    VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020.INSTANCE
  )

  def getSecretResolverInMemory: SecretResolverInMemory = {
    val keyIdAgreement = PeerDIDUtils.createMultibaseEncnumbasis(keyAgreement).drop(1)
    val keyIdAuthentication = PeerDIDUtils.createMultibaseEncnumbasis(keyAuthentication).drop(1)

    val secretKeyAgreement = new Secret(
      s"${did.string}#$keyIdAgreement",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAgreement.toJWK.toJSONString)
    )
    val secretKeyAuthentication = new Secret(
      s"${did.string}#$keyIdAuthentication",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAuthentication.toJWK.toJSONString)
    )

    new SecretResolverInMemory(
      Map(
        s"${did.string}#$keyIdAgreement" -> secretKeyAgreement,
        s"${did.string}#$keyIdAuthentication" -> secretKeyAuthentication,
      ).asJava
    )
  }

  def getDIDDocument = org.didcommx.peerdid.PeerDIDResolver
    .resolvePeerDID(did.string, VerificationMaterialFormatPeerDID.JWK)
}

//     val str = "z6MkvP1xH6gefWhaxjh4fCf6xyz1BeYdv9hBwx2jsCsVQaZ9"

//     {
//       import io.ipfs.multibase.Multibase
//       val aaa = Multibase.decode(str)
//       println(aaa.toSeq.mkString(" "))
//       println(String(Base64.getUrlEncoder.encode(aaa.drop(2))))
//       // val bbb = Multibase.encode(Multibase.Base.Base32Hex, aaa)
//       // println(bbb)
//       // println(bbb.length())
//       val ccc = Multibase.encode(Multibase.Base.Base64, aaa.drop(2))
//       println(ccc)
//     }

@main def mainDidcommxPeerdid() = {
  val did = DidcommxPeerdid(serviceEndpoint = Some("http://localhost:8080/test"))
  println(did)
  println(did.did.string)
  println(did.getDIDDocument)
}
