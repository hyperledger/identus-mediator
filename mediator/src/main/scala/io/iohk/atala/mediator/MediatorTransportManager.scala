package io.iohk.atala.mediator

import zio._
import zio.json._
import zio.stream._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.framework._
import fmgp.crypto.error._
import fmgp.util._

type TransportID = String

/** Based on the [[fmgp.did.framework.TransportDispatcher]] */
case class MediatorTransportManager(
    transports: Set[TransportDIDComm[Any]] = Set.empty,
    ids: Map[FROMTO, Set[TransportID]] = Map.empty,
    kids: Map[VerificationMethodReferenced, Set[TransportID]] = Map.empty,
    liveMode: Map[FROMTO, Set[TransportID]] = Map.empty,
    transportFactory: TransportFactory
) extends TransportDispatcher {

  override def openTransport(uri: String): UIO[TransportDIDComm[Any]] =
    transportFactory.openTransport(uri) // FIXME TODO register Transport

  def link(vmr: VerificationMethodReferenced, transportID: TransportID): MediatorTransportManager =
    if (!transports.map(_.id).contains(transportID)) this // if transport is close
    else
      kids.get(vmr) match
        case Some(seq) if seq.contains(transportID) => this
        case Some(seq) => this.copy(kids = kids + (vmr -> (seq + transportID))).link(vmr.did.asFROMTO, transportID)
        case None      => this.copy(kids = kids + (vmr -> Set(transportID))).link(vmr.did.asFROMTO, transportID)

  def link(from: FROMTO, transport: TransportDIDComm[Any]): MediatorTransportManager = link(from, transport.id)
  def link(from: FROMTO, transportID: TransportID): MediatorTransportManager =
    if (!transports.map(_.id).contains(transportID)) this // if transport is close
    else
      ids.get(from) match
        case Some(seq) if seq.contains(transportID) => this
        case Some(seq)                              => this.copy(ids = ids + (from -> (seq + transportID)))
        case None                                   => this.copy(ids = ids + (from -> Set(transportID)))

  def registerTransport(transport: TransportDIDComm[Any]) =
    this.copy(transports = transports + transport)

  def unregisterTransport(transportID: TransportID) = this.copy(
    transports = transports.filter(_.id != transportID),
    ids = ids.map { case (did, ids) => (did, ids.filter(_ != transportID)) }.filterNot(_._2.isEmpty),
    kids = kids.map { case (kid, ids) => (kid, ids.filter(_ != transportID)) }.filterNot(_._2.isEmpty),
    liveMode = liveMode.map { case (did, ids) => (did, ids.filter(_ != transportID)) }.filterNot(_._2.isEmpty),
  )

  def enableLiveMode(subject: FROMTO, transportID: TransportID): MediatorTransportManager =
    this.copy(
      liveMode = liveMode.updatedWith(subject) {
        case Some(set) => Some(set + transportID)
        case None      => Some(Set(transportID))
      }
    )

  def disableLiveMode(subject: FROMTO, transportID: TransportID): MediatorTransportManager =
    this.copy(
      liveMode = liveMode.updatedWith(subject) {
        case Some(set) => Some(set - transportID).filter(_.isEmpty)
        case None      => None
      }
    )

  def isLiveModeEnabled(subject: FROMTO, transportID: TransportID): Boolean =
    liveMode.get(subject) match {
      case Some(set) => set.contains(transportID)
      case None      => false
    }

  def getLiveModeEnableConnections(subject: FROMTO): Seq[TransportDIDComm[Any]] =
    liveMode.get(subject).toSeq.flatMap(transportId => transports.filter(t => transportId.contains(t.id)))

  def sendForLiveMode(
      next: FROMTO,
      msg: /*lazy*/ => SignedMessage | EncryptedMessage
  ): ZIO[Any, DidFail, Unit] =
    for {
      transportIDs <- ZIO.succeed(this.liveMode.getOrElse(next, Set.empty))
      myChannels <- ZIO.succeed(transportIDs.flatMap(id => this.transports.find(_.id == id)))
      _ <- ZIO.foreach(myChannels) { _.send(msg) }
    } yield ()

  // TODO maybe rename to send
  def publish(to: TO, msg: SignedMessage | EncryptedMessage): ZIO[Any, Nothing, Iterable[Unit]] = {
    val transportIDs = this.ids.getOrElse(to.asFROMTO, Seq.empty)
    val myChannels = transportIDs.flatMap(id => this.transports.find(_.id == id))
    ZIO.foreach(myChannels) { _.send(msg) }
  }

  override def send(
      to: TO,
      msg: SignedMessage | EncryptedMessage,
      thid: Option[MsgID], // TODO use
      pthid: Option[MsgID], // TODO use
  ): ZIO[Resolver & Agent & Operations, DidFail, Unit] =
    sendViaDIDCommMessagingService(to, msg).unit

  override def sendViaDIDCommMessagingService(
      to: TO,
      msg: SignedMessage | EncryptedMessage
  ): ZIO[Resolver & Agent & Operations, DidFail, Either[String, TransportDIDComm[Any]]] =
    super.sendViaDIDCommMessagingService(to, msg)

}

object MediatorTransportManager {

  def make: URIO[TransportFactory, Ref[MediatorTransportManager]] =
    for {
      transportFactory <- ZIO.service[TransportFactory]
      ref <- Ref.make(MediatorTransportManager(transportFactory = transportFactory))
    } yield ref

  def registerTransport(transport: TransportDIDComm[Any]) =
    for {
      socketManager <- ZIO.service[Ref[MediatorTransportManager]]
      _ <- socketManager.update { _.registerTransport(transport) }
      _ <- ZIO.log(s"RegisterTransport concluded")
    } yield ()

  def unregisterTransport(transportId: String) =
    for {
      socketManager <- ZIO.service[Ref[MediatorTransportManager]]
      _ <- socketManager.update { case sm: MediatorTransportManager => sm.unregisterTransport(transportId) }
      _ <- ZIO.log(s"Channel unregisterSocket")
    } yield ()

}
