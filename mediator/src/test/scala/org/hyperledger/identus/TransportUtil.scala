package org.hyperledger.identus.mediator

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.framework._
import zio.stream._

object TransportUtil {
  def newTransportEmpty: TransportDIDComm[Any] =
    new Transport[Any, SignedMessage | EncryptedMessage, SignedMessage | EncryptedMessage] {
      def transmissionFlow = ???
      def transmissionType = ???
      def id: TransportID = "newTransportEmpty_Test"
      def inbound: zio.stream.ZStream[Any, Transport.InErr, SignedMessage | EncryptedMessage] = ???
      def outbound: zio.stream.ZSink[Any, Transport.OutErr, SignedMessage | EncryptedMessage, Nothing, Unit] = ???
    }

  def newTransportEmptySingleTransmission: TransportDIDComm[Any] =
    new Transport[Any, SignedMessage | EncryptedMessage, SignedMessage | EncryptedMessage] {

      def transmissionFlow = Transport.TransmissionFlow.BothWays
      def transmissionType = Transport.TransmissionType.SingleTransmission

      def id: TransportID = "newTransportEmpty_Test_SingleTransmission"
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

  def newTransportEmptyMultiTransmissions: TransportDIDComm[Any] =
    new Transport[Any, SignedMessage | EncryptedMessage, SignedMessage | EncryptedMessage] {

      def transmissionFlow = Transport.TransmissionFlow.BothWays
      def transmissionType = Transport.TransmissionType.MultiTransmissions

      def id: TransportID = "newTransportEmpty_Test_MultiTransmissions"
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
