package fmgp.did.method.web

import zio._
import fmgp.did._

/** DID web
  *
  * @see
  *   https://w3c-ccg.github.io/did-method-web/
  */
case class DIDWeb(subject: DIDSubject) extends DID {
  assert(subject.toDID.namespace == "web")
  def namespace: String = subject.toDID.namespace
  def specificId: String = subject.toDID.specificId

  def domainName: String = specificId.split(":").head // NOTE split always have head

  def paths: Array[String] = specificId.split(":").drop(1)

  def url = "https://" + domainName.replace("%3A", ":") + "/" + {
    if (paths.isEmpty) ".well-known" else paths.mkString("/")
  } + "/did.json"

}

object DIDWeb {
  def applyUnsafe(did: String): DIDWeb = DIDWeb(DIDSubject(did))
}
