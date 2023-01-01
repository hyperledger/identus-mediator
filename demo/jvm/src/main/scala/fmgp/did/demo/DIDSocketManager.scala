package fmgp.did.demo

import zio._
import zio.http.{Channel, ChannelEvent}
import zio.http.socket._
import zio.stream._

import fmgp.did._

type SocketID = String
case class MyChannel(id: SocketID, socketOutHub: Hub[String])
case class DIDSocketManager(
    host: String,
    sockets: Map[SocketID, MyChannel] = Map.empty,
    ids: Map[DIDSubject, SocketID] = Map.empty,
)

object DIDSocketManager {
  def inBoundSize = 5
  def outBoundSize = 3

  val aliceLayer = ZLayer(
    for {
      inboundHub <- Hub.bounded[String](inBoundSize)
      ref <- Ref.make(DIDSocketManager("alice.did.fmgp.app"))
    } yield (ref)
  )

  def addNewSockets(channel: Channel[WebSocketFrame]) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]] // ZState ???
      hub <- Hub.bounded[String](outBoundSize)
      myChannel = MyChannel(channel.id, hub)
      _ <- socketManager.update { sm => sm.copy(sockets = sm.sockets + (myChannel.id -> myChannel)) }
      sink = ZSink.foreach((value: String) => channel.writeAndFlush(WebSocketFrame.text(value)))
      _ <- ZStream
        .fromHub(myChannel.socketOutHub)
        // .tap(data => Console.printLine(s"### $data"))
        .run(sink)
      // _ <- myChannel.socketOutHub.publish(s"HandshakeComplete (${myChannel.id})")
      _ <- Console.printLine(s"Channel(${channel.id}): addNewSockets Done")
    } yield ()

  // def newMessage(ce: ChannelEvent[WebSocketFrame, WebSocketFrame.Text]) =
  def newMessage(channel: Channel[WebSocketFrame], data: String) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      id = channel.id
      _ <-
        if (data.startsWith("$")) {
          Console.printLine(s"Channel($id) - add ID: $data")
            *> socketManager.update(sm => sm.copy(ids = sm.ids + (DIDSubject(data) -> id)))
        } else {
          Console.printLine(s"Channel($id) - newMessage: $data") *>
            socketManager.get.flatMap { sm =>
              ZIO.foreach(
                sm.sockets.map(_._2.socketOutHub.publish(s"Repay: $data"))
              )(e => e)
            }
        }
    } yield ()

  val socketApp: SocketApp[Ref[DIDSocketManager]] = SocketApp {
    case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
      DIDSocketManager.addNewSockets(ch)
    case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
      DIDSocketManager.newMessage(ch, text)
    case ch =>
      Console.printLine("TODO" + ch.event)
  }

}
