package fmgp.did.demo

import zio._
import zio.json._
import zio.http.{Channel, ChannelEvent}
import zio.http.socket._
import zio.stream._

import fmgp.did._

type SocketID = String
case class MyChannel(id: SocketID, socketOutHub: Hub[String])
case class DIDSocketManager(
    host: String,
    sockets: Map[SocketID, MyChannel] = Map.empty,
    ids: Map[DIDSubject, Seq[SocketID]] = Map.empty,
)

object DIDSocketManager {
  def inBoundSize = 5
  def outBoundSize = 3

  private given JsonEncoder[Hub[String]] = JsonEncoder.string.contramap((e: Hub[String]) => "HUB")
  private given JsonEncoder[MyChannel] = DeriveJsonEncoder.gen[MyChannel]
  given encoder: JsonEncoder[DIDSocketManager] = DeriveJsonEncoder.gen[DIDSocketManager]

  val aliceLayer = ZLayer(
    for {
      inboundHub <- Hub.bounded[String](inBoundSize)
      ref <- Ref.make(DIDSocketManager("alice.did.fmgp.app"))
    } yield (ref)
  )

  def registerSocket(channel: Channel[WebSocketFrame]) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]] // ZState ???
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
      _ <-
        if (data.startsWith("$")) {
          ZIO.logDebug(s"Channel($id) - add ID: $data")
            *> socketManager.update { sm =>
              val did = DIDSubject(data)
              val sockets = sm.ids.get(did).getOrElse(Seq.empty) :+ id
              sm.copy(ids = sm.ids + (did -> sockets))
            }
        } else {
          ZIO.logDebug(s"Channel($id) - newMessage: $data") *>
            socketManager.get.flatMap { sm =>
              ZIO.foreach(
                sm.sockets.map(_._2.socketOutHub.publish(s"Repay: $data"))
              )(e => e)
            }
        }
    } yield ()

  def unregisterSocket(channel: Channel[WebSocketFrame]) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]] // ZState ???
      _ <- socketManager.update { case DIDSocketManager(host, sockets, ids) =>
        DIDSocketManager(
          host = host,
          sockets = sockets.filterKeys(_ != channel.id).toMap,
          ids = ids.map { case (did, socketsID) => (did, socketsID.filter(_ != channel.id)) }.filterNot(_._2.isEmpty)
        )
      }
      _ <- ZIO.logDebug(s"Channel(${channel.id}): unregisterSocket Done")
    } yield ()

  val socketApp: SocketApp[Ref[DIDSocketManager]] = SocketApp {
    case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
      DIDSocketManager.registerSocket(ch)
    case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
      DIDSocketManager.newMessage(ch, text)
    case ChannelEvent(ch, ChannelEvent.ChannelUnregistered) =>
      DIDSocketManager.unregisterSocket(ch)
    case ch =>
      Console.printLine("TODO" + ch.event)
  }

}
