package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.util.MyHeaders

trait MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
      xForwardedHost: Option[String],
  ): ZIO[Any, DidFail, String]
}
