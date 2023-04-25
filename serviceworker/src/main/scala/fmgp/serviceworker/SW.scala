package fmgp.serviceworker

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import scalajs._
import org.scalajs.dom._

import zio.json._
import scala.concurrent.Promise

// import javascriptLogo from "/javascript.svg"
// @js.native @JSImport("/javascript.svg", JSImport.Default)
// val javascriptLogo: String = js.native

// @main
// def SW(): Unit =
// end SW

object SW {
  // def main(args: Array[String]): Unit = println("main")
  val version = 16

  def main() = {
    println(s"--- SW version $version ---")
    ServiceWorkerGlobalScope.self.oninstall = (event: ExtendableEvent) => oninstall(event)
    ServiceWorkerGlobalScope.self.onactivate = (event: ExtendableEvent) => onactivate(event)
    ServiceWorkerGlobalScope.self.onmessage = (event: MessageEvent) => println("onmessage")
    ServiceWorkerGlobalScope.self.onpush = (event: PushEvent) => onpush(event)
    ServiceWorkerGlobalScope.self.onpushsubscriptionchange = (event: PushEvent) => println("onpushsubscriptionchange")
    ServiceWorkerGlobalScope.self.ononline = (event: Event) => println("ononline")
    ServiceWorkerGlobalScope.self.onoffline = (event: Event) => println("onoffline")
    NotificationUtils.notificationclickListener
  }

  def oninstall(event: ExtendableEvent) = {
    println("SW: Installing")
    event.waitUntil(
      ServiceWorkerGlobalScope.self.caches.toOption
        .map { cacheStorage =>
          cacheStorage
            .`open`("v1")
            .`then`((cache) =>
              cache.addAll(
                js.Array(
                  "/",
                  "/public/fmgp-webapp-fastopt-bundle.js",
                  // "/public/fmgp-webapp-fastopt-library.js",
                  "https://fonts.googleapis.com/css?family=Roboto:300,400,500",
                  "https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css",
                  "https://fonts.googleapis.com/icon?family=Material+Icons",
                )
              )
            )
            .`then`(_ => ServiceWorkerGlobalScope.self.skipWaiting())

        }
        .getOrElse(js.Promise.resolve(()))
    )
  }

  // New service worker installed ( now you can safely perform deleting/migrating old cache)
  def onactivate(event: ExtendableEvent) = {
    println("SW: activating")
    event.waitUntil(
      ServiceWorkerGlobalScope.self.caches.toOption
        .map { cacheStorage =>
          cacheStorage
            .`open`("v1")
            .`then`((cache) =>
              cache.keys().`then`(cacheNames => js.Promise.all(cacheNames.map(cacheName => cache.delete(cacheName))))
            )
        }
        .getOrElse(js.Promise.resolve(()))
    )
  }

  // https://developer.mozilla.org/en-US/docs/Web/API/PushEvent
  def onpush(event: PushEvent) = {
    if (js.isUndefined(event.data)) println("Push event but no data")
    else {
      println("SW: onpush: " + event.data.text())
      val notificationMessage = event.data.text().fromJson[NotificationMessage] match
        case Left(error)  => println(s"SW: fail to parse the NotificationMessage due to '$error'")
        case Right(value) => NotificationUtils.showNotification(value)
    }
  }

}
