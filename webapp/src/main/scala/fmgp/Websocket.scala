package fmgp

import org.scalajs.dom.{CloseEvent, Event, MessageEvent, WebSocket}

import scala.scalajs.js
import zio._

//case class World(data: String)

object Websocket {
  type State = State.Value
  object State extends Enumeration {

    /** Socket has been created. The connection is not yet open. */
    val CONNECTING = Value(0)

    /** The connection is open and ready to communicate. */
    val OPEN = Value(1)

    /** The connection is in the process of closing. */
    val CLOSING = Value(2)

    /** The connection is closed or couldn't be opened. */
    val CLOSED = Value(3)
  }

  case class AutoReconnect[WS <: WebsocketJS](
      wsUrl: String,
      wsJS: WS,
      defualtReconnectDelay: Int = 20000,
      var ws: js.UndefOr[WebSocket] = js.undefined,
  ) {
    println(wsUrl)
    connect(0)

    val wsLayer: ULayer[WebsocketJS] = ZLayer.succeed[WebsocketJS](wsJS)

    def getState: State = ws.map(e => State(e.readyState)).getOrElse(State.CLOSED)

    /** @see https://japgolly.github.io/scalajs-react/#examples/websockets */
    private def connect(delay: Int): Unit = {
      // log.info(s"WS try reconect to $wsUrl (in ${delay / 1000} s)")
      js.timers.setTimeout(delay) {
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.runToFuture(WebsocketJS.onStateChange(getState).provide(wsLayer))
          // .getOrThrowFiberFailure()
        }

        val tmpWS = new WebSocket(wsUrl) // TODO Add a timeout here
        ws = tmpWS

        tmpWS.onopen = { (ev: Event) =>
          Unsafe.unsafe { implicit unsafe => // Run side efect
            Runtime.default.unsafe.runToFuture({
              WebsocketJS.onStateChange(Websocket.State.OPEN) *>
                WebsocketJS.onOpen(ev.`type`)
            }.provide(wsLayer))
            // .getOrThrowFiberFailure()
          }
        }
        tmpWS.onclose = { (ev: CloseEvent) =>
          Unsafe.unsafe { implicit unsafe => // Run side efect
            Runtime.default.unsafe.runToFuture(
              {
                WebsocketJS.onStateChange(Websocket.State.CLOSED) *>
                  WebsocketJS.onClose(ev.reason)
              }.provide(wsLayer)
                .map(_ => connect(defualtReconnectDelay))
            )
            // .getOrThrowFiberFailure().map(_ => connect(defualtReconnectDelay))
          }
        }
        tmpWS.onmessage = { (ev: MessageEvent) =>
          Unsafe.unsafe { implicit unsafe => // Run side efect
            Runtime.default.unsafe.runToFuture(WebsocketJS.onMessage(message = ev.data.toString).provide(wsLayer))
            // .getOrThrowFiberFailure()
          }
        }
        tmpWS.onerror = { (ev: Event) => // TODO ErrorEvent
          val message = ev
            .asInstanceOf[js.Dynamic]
            .message
            .asInstanceOf[js.UndefOr[String]]
            .fold("")("Error: " + _)
          Unsafe.unsafe { implicit unsafe => // Run side efect
            Runtime.default.unsafe.runToFuture(WebsocketJS.onError(ev.`type`, message).provide(wsLayer))
            // .getOrThrowFiberFailure()
          }
        }
      }
    }
  }
}
