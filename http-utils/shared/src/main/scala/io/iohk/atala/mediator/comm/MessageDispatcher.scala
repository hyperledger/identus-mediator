package io.iohk.atala.mediator.comm

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import io.iohk.atala.mediator.utils.MyHeaders
import zio.*
import zio.json.*

trait MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
      xForwardedHost: Option[String],
  ): ZIO[Any, DispatcherError, String]
}
