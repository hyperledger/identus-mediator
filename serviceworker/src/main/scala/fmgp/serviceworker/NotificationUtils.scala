package fmgp.serviceworker

import scala.scalajs.js

import org.scalajs.dom._

object NotificationUtils {
  def hasNotificationPermission = Notification.permission == "granted"

  def showNotification(notificationMessage: NotificationMessage) = {
    if (hasNotificationPermission) {
      val options = {
        val tmp = new NotificationOptions {
          val actions = js.Array(
            // TODO https://web.dev/push-notifications-notification-behaviour/
            js.Dictionary(
              ("action", "open"),
              ("type", "button"),
              ("title", "Open"),
              ("icon", "favicon.ico"),
            ),
            js.Dictionary(
              ("action", "reply"),
              ("type", "text"),
              ("title", "Reply"),
              ("placeholder", "Type your reply here"),
            ),
          )
        }
        tmp.body = notificationMessage.body
        tmp.tag = "SomeTag"
        tmp.renotify = true
        tmp.icon = "favicon.ico"
        tmp
      }
      //     // notification.addEventListener("click", () => {
      //     //   clients.openWindow(
      //     //     "https://example.blog.com/2015/03/04/something-new.html"
      //     //   );
      //     // });
      ServiceWorkerGlobalScope.self.registration
        .showNotification(
          notificationMessage.title,
          options
        )
    }
  }

  def notificationclickListener = ServiceWorkerGlobalScope.self.addEventListener(
    "notificationclick",
    (event: NotificationEvent) => {
      val clickedNotification = event.notification
      clickedNotification.close()
      // Do something as the result of the notification click
      event.waitUntil(
        js.Promise.resolve {
          println(s"notification click ${event.action} - ${event.reply}")
        }
      )
    }
  )
}
