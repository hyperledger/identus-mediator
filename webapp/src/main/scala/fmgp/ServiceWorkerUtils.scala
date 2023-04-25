package fmgp

import zio._
import zio.json._

import org.scalajs.dom._
import org.scalajs.dom.experimental.push._
import org.scalajs.dom.experimental.serviceworkers._
import org.scalajs.dom.experimental.{Notification, NotificationOptions}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Random, Success}

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.Uint8Array
import fmgp.did.Client

case class NotificationsSubscription(endpoint: String, keyP256DH: String, keyAUTH: String, id: Option[String]) {
  def name: String = id.getOrElse("?_?")
}

object NotificationsSubscription {
  given decoder: JsonDecoder[NotificationsSubscription] = DeriveJsonDecoder.gen[NotificationsSubscription]
  given encoder: JsonEncoder[NotificationsSubscription] = DeriveJsonEncoder.gen[NotificationsSubscription]
}

def base642Uint8Array(base64: String): Uint8Array =
  byteArray2Uint8Array(java.util.Base64.getUrlDecoder.decode(base64))

def byteArray2Uint8Array(arr: Array[Byte]): Uint8Array =
  js.Dynamic.newInstance(js.Dynamic.global.Uint8Array)(arr.toJSArray).asInstanceOf[Uint8Array]

@scala.scalajs.js.annotation.JSExportTopLevel("ServiceWorkerUtils")
object ServiceWorkerUtils {

  /** //navigator.serviceWorker.register() is effectively a no-op during subsequent visits. When it's called is
    * irrelevant.
    *
    * @see
    *   https://developers.google.com/web/fundamentals/primers/service-workers/lifecycle
    */
  @scala.scalajs.js.annotation.JSExport
  def registerServiceWorker: Future[Unit] = {
    println("### call to registerServiceWorker")
    window.navigator.serviceWorker
      .register("/sw.js")
      .toFuture
      .flatMap { registration =>
        println("registerServiceWorker: registered service worker")
        registration
          .update()
          .toFuture
          .andThen { case _ => subscribeToNotifications }
      }
      .recover { case error =>
        println(s"registerServiceWorker: service worker registration failed > $error: ${error.printStackTrace()}")
      }
  }

  @scala.scalajs.js.annotation.JSExport
  def runSubscribeToNotifications =
    Unsafe.unsafe { implicit unsafe => // Run side efect
      Runtime.default.unsafe.runToFuture({
        subscribeToNotifications
      })
      // .getOrThrowFiberFailure()
    }

  def subscribeToNotifications = {
    ZIO.fromPromiseJS(window.navigator.serviceWorker.ready).map { registration =>
      val pushManager: PushManager = registration.pushManager

      val pushSubscriptionOptions = new PushSubscriptionOptions {
        userVisibleOnly = true
        applicationServerKey = base642Uint8Array(fmgp.Config.PushNotifications.applicationServerKey)
      }

      pushManager.subscribe(pushSubscriptionOptions).toFuture.onComplete {
        case Failure(exception) => println(s"Fail to subscribeToNotifications with exception: $exception")
        case Success(value)     => println(s"Subscribe To Notifications return: ${js.JSON.stringify(value.toJSON())}")
      }
    }
  }

  @scala.scalajs.js.annotation.JSExport
  def runPushNotificationsSubscription(id: String) =
    Unsafe.unsafe { implicit unsafe => // Run side efect
      Runtime.default.unsafe.runToFuture({
        pushNotificationsSubscription(id)
      })
      // .getOrThrowFiberFailure()
    }

  def pushNotificationsSubscription(id: String) = {

    ZIO.fromPromiseJS(window.navigator.serviceWorker.ready).map { registration =>
      val pushManager: PushManager = registration.pushManager
      pushManager.getSubscription().toFuture.onComplete {
        case Failure(exception) => println(s"Fail to getNotificationsSubscription with exception: $exception")
        case Success(value) =>
          val ns = NotificationsSubscription(
            endpoint = value.endpoint,
            keyP256DH = value.toJSON().keys.get("p256dh").get,
            keyAUTH = value.toJSON().keys.get("auth").get,
            id = if (id.trim.isEmpty) None else Some(id.trim)
          )
          println(s"My NotificationsSubscription is: ${js.JSON.stringify(value.toJSON())}")
          Client.pushNotificationsSubscription(ns)
      }
    }
  }

  @scala.scalajs.js.annotation.JSExport
  def requestNotificationPermission = {
    def aux(status: String) = println(s"Notification permission status: $status")
    dom.Notification.requestPermission(aux _)
  }

}
