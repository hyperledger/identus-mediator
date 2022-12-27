package fmgp.webapp

import scala.scalajs.js.JSConverters._

import zio.json._
import zio.json.ast.Json
import scala.scalajs.js
import org.scalajs.dom

@js.annotation.JSExportTopLevel("IpfsClient")
object IpfsClient {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  // val url = "http://localhost:5001"
  val url = "http://192.168.1.78:9081"

  @js.annotation.JSExport
  def dagGet(cid: String = "bafyreihn56com4a5yt2yjt6renhfdakx7ntkyg7huethm46inaxkd2acwi") = {

    val requestInit =
      js.Dynamic
        .literal(method = dom.HttpMethod.POST)
        .asInstanceOf[dom.RequestInit]

    dom.Fetch
      .fetch(s"$url/api/v0/dag/get?arg=$cid", requestInit)
      .toFuture
      .flatMap(_.text().toFuture)
      .map(_.fromJson[Statement])
    // .map(e => println(e))

    // {"<second-part-of-the-signedCredential>":"MEYCIQDmJrnwAbieaH9f28FDae1oMyyS-DjpNNO9NUipZd592QIhAL2tVnQbx8P-44IM1NSmvnJM49boW2Vy-_jR-pbwn1vq","credential":{"credentialSubject":{"CID":{"/":"QmX6CvErxHkuybopMsRYC3oHZvsrkbLQpYkfzCvP8nNFfi"},"category":"UPTODATE","id":"did:prism:e6dfb26d195076c6408ae479eae6a128ab6f859f0d1c296f602b3dba6a3e714b"},"id":"did:prism:e6dfb26d195076c6408ae479eae6a128ab6f859f0d1c296f602b3dba6a3e714b","keyId":"issuing0"},"proof":{"hash":"6a1c8db293ff793f086dd0544a76a09fe0ebf5ca15fee5431dab60321cec0e60","index":0,"siblings":[]}}
  }
}

sealed trait Statements
object Statements {
  implicit val decoder: JsonDecoder[Statements] = DeriveJsonDecoder.gen[Statements]
  implicit val encoder: JsonEncoder[Statements] = DeriveJsonEncoder.gen[Statements]
}

case class ManyStatements(list: Array[Statements]) extends Statements
object ManyStatements {
  implicit val decoder: JsonDecoder[ManyStatements] = DeriveJsonDecoder.gen[ManyStatements]
  implicit val encoder: JsonEncoder[ManyStatements] = DeriveJsonEncoder.gen[ManyStatements]
}
case class Statement(
    `<second-part-of-the-signedCredential>`: String,
    credential: Credential,
    proof: Proof,
) extends Statements
object Statement {
  implicit val decoder: JsonDecoder[Statement] = DeriveJsonDecoder.gen[Statement]
  implicit val encoder: JsonEncoder[Statement] = DeriveJsonEncoder.gen[Statement]
}
case class Proof(hash: String, index: Int, siblings: Seq[String])
object Proof {
  implicit val decoder: JsonDecoder[Proof] = DeriveJsonDecoder.gen[Proof]
  implicit val encoder: JsonEncoder[Proof] = DeriveJsonEncoder.gen[Proof]
}
case class Credential(id: String, keyId: String, credentialSubject: CredentialSubject)
object Credential {
  implicit val decoder: JsonDecoder[Credential] = DeriveJsonDecoder.gen[Credential]
  implicit val encoder: JsonEncoder[Credential] = DeriveJsonEncoder.gen[Credential]
}
case class CredentialSubject(id: String, CID: MyCID, category: String)

object CredentialSubject {
  implicit val decoder: JsonDecoder[CredentialSubject] = DeriveJsonDecoder.gen[CredentialSubject]
  implicit val encoder: JsonEncoder[CredentialSubject] = DeriveJsonEncoder.gen[CredentialSubject]
}

case class MyCID(`/`: String)
object MyCID {
  implicit val decoder: JsonDecoder[MyCID] = DeriveJsonDecoder.gen[MyCID]
  implicit val encoder: JsonEncoder[MyCID] = DeriveJsonEncoder.gen[MyCID]
}
