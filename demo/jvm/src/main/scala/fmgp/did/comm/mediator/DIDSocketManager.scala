package fmgp.did.comm.mediator

import zio._
import zio.json._
import zio.http.{Channel, ChannelEvent}
import zio.http.socket._
import zio.stream._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

type SocketID = String
case class MyChannel(id: SocketID, socketOutHub: Hub[String])
case class DIDSocketManager(
    // host: String,
    sockets: Map[SocketID, MyChannel] = Map.empty,
    ids: Map[FROMTO, Seq[SocketID]] = Map.empty,
    kids: Map[VerificationMethodReferenced, Seq[SocketID]] = Map.empty,
) {

  def link(from: VerificationMethodReferenced, socketID: SocketID): DIDSocketManager =
    if (!sockets.keySet.contains(socketID)) this // if sockets is close
    else
      kids.get(from) match
        case Some(seq) if seq.contains(socketID) => this
        case Some(seq) => this.copy(kids = kids + (from -> (seq :+ socketID))).link(from.fromto, socketID)
        case None      => this.copy(kids = kids + (from -> Seq(socketID))).link(from.fromto, socketID)

  def link(from: FROMTO, socketID: SocketID): DIDSocketManager =
    if (!sockets.keySet.contains(socketID)) this // if sockets is close
    else
      ids.get(from) match
        case Some(seq) if seq.contains(socketID) => this
        case Some(seq)                           => this.copy(ids = ids + (from -> (seq :+ socketID)))
        case None                                => this.copy(ids = ids + (from -> Seq(socketID)))
}

object DIDSocketManager {
  def inBoundSize = 5
  def outBoundSize = 3

  private given JsonEncoder[Hub[String]] = JsonEncoder.string.contramap((e: Hub[String]) => "HUB")
  private given JsonEncoder[MyChannel] = DeriveJsonEncoder.gen[MyChannel]
  given encoder: JsonEncoder[DIDSocketManager] = DeriveJsonEncoder.gen[DIDSocketManager]

  def make = Ref.make(DIDSocketManager())

  def registerSocket(channel: Channel[WebSocketFrame]) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      hub <- Hub.bounded[String](outBoundSize)
      myChannel = MyChannel(channel.id, hub)
      _ <- socketManager.update { sm => sm.copy(sockets = sm.sockets + (myChannel.id -> myChannel)) }
      sink = ZSink.foreach((value: String) => channel.writeAndFlush(WebSocketFrame.text(value)))
      _ <- ZStream.fromHub(myChannel.socketOutHub).run(sink)
      _ <- ZIO.logDebug(s"Channel(${channel.id}): registerSocket Done")
    } yield ()

  def newMessage(channel: Channel[WebSocketFrame], data: String) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      id = channel.id
    } yield (id, data)

  def unregisterSocket(channel: Channel[WebSocketFrame]) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      _ <- socketManager.update { case DIDSocketManager(sockets, ids, kids) =>
        DIDSocketManager(
          sockets = sockets.view.filterKeys(_ != channel.id).toMap,
          ids = ids.map { case (did, socketsID) => (did, socketsID.filter(_ != channel.id)) }.filterNot(_._2.isEmpty),
          kids = kids.map { case (kid, socketsID) => (kid, socketsID.filter(_ != channel.id)) }.filterNot(_._2.isEmpty)
        )
      }
      _ <- ZIO.logDebug(s"Channel(${channel.id}): unregisterSocket Done")
    } yield ()

}
