package fmgp

import zio._
import zio.json._
import fmgp.did.comm._
import com.raquo.airstream.state.Var

trait WebsocketJS {
  def onOpen(evType: String): UIO[Unit] = Console.printLine(s"WS Connected '$evType'").orDie
  def onClose(reason: String): UIO[Unit] = Console.printLine(s"WS Closed because '${reason}'").orDie
  def onMessage(message: String): UIO[Unit]
  def onError(evType: String, errorMessage: String): UIO[Unit] =
    Console.printLine(s"WS Error (type:$evType) occurred! " + errorMessage).orDie

  // Extra
  def onStateChange(state: Websocket.State): UIO[Unit]
}

object WebsocketJS {
  // Accessor Methods Inside the Companion Object
  def onOpen(evType: String): URIO[WebsocketJS, Unit] = ZIO.serviceWithZIO(_.onOpen(evType))
  def onClose(reason: String): URIO[WebsocketJS, Unit] = ZIO.serviceWithZIO(_.onClose(reason))
  def onMessage(message: String): URIO[WebsocketJS, Unit] = ZIO.serviceWithZIO(_.onMessage(message))
  def onError(evType: String, message: String): URIO[WebsocketJS, Unit] =
    ZIO.serviceWithZIO(_.onError(evType: String, message: String))
  def onStateChange(newState: Websocket.State): URIO[WebsocketJS, Unit] = ZIO.serviceWithZIO(_.onStateChange(newState))
}

case class WebsocketJSLive(
    income: Var[Seq[TapMessage]],
    state: Var[Option[Websocket.State]],
) extends WebsocketJS {
  override def onMessage(message: String): UIO[Unit] =
    message.fromJson[TapMessage] match
      case Left(ex) => Console.printLine(message).orDie *> Console.printLine(s"Error parsing the obj World: $ex").orDie
      case Right(value) => ZIO.succeed(income.update(s => s :+ value))

  override def onStateChange(newState: Websocket.State): UIO[Unit] = ZIO.succeed(state.update(_ => Some(newState)))
}

object WebsocketJSLive {

  import scalajs.js.internal.UnitOps.unitOrOps // This shound not be needed

  val wsUrl =
    org.scalajs.dom.window.location.origin
      .getOrElse("http://localhost:8080")
      .replaceFirst("http", "ws") + "/tap/alice.did.fmgp.app"

  val messages = Var[Seq[TapMessage]](initial = Seq.empty)
  val state = Var[Option[Websocket.State]](initial = None)

  lazy val autoReconnect = Websocket.AutoReconnect(wsUrl, WebsocketJSLive(messages, state))
}
