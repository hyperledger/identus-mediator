package io.iohk.atala.mediator

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.framework._
import zio.stream._

object TransportUtil {
  def newTransportEmpty: TransportDIDComm[Any] =
    new Transport[Any, SignedMessage | EncryptedMessage, SignedMessage | EncryptedMessage] {

      def id: TransportID = "newTransportEmpty_Test"
      def inbound: zio.stream.ZStream[
        Any,
        Transport.InErr,
        SignedMessage | EncryptedMessage
      ] = ZStream.empty
      def outbound: zio.stream.ZSink[
        Any,
        Transport.OutErr,
        SignedMessage | EncryptedMessage,
        Nothing,
        Unit
      ] = ZSink.drain

    }
}
