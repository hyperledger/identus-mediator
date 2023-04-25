package fmgp.serviceworker

import scala.scalajs.js
import scala.scalajs.js.annotation._

import zio.json._

case class NotificationMessage(title: String, body: String)
object NotificationMessage {
  given JsonDecoder[NotificationMessage] = DeriveJsonDecoder.gen[NotificationMessage]
  given JsonEncoder[NotificationMessage] = DeriveJsonEncoder.gen[NotificationMessage]
}
