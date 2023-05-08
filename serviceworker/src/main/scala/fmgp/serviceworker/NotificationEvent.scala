package fmgp.serviceworker

import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom._

/** See [[https://developer.mozilla.org/en-US/docs/Web/API/NotificationEvent]] of whatwg ServiceWorker spec.
  *
  * An ExtendableEvent object has an associated extend lifetime promises (an array of promises). It is initially set to
  * null.
  */
@js.native
@JSGlobal
class NotificationEvent(typeArg: String, init: js.UndefOr[ExtendableEventInit]) extends ExtendableEvent(typeArg, init) {

  /** A new Notification object representing notification. */
  def notification: Notification = js.native

  def action: String = js.native // TODO type DOMString

  // https://api.flutter.dev/flutter/dart-html/NotificationEvent/reply.html
  def reply: js.UndefOr[String] = js.native
}
