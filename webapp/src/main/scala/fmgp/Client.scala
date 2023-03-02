package fmgp.did

import scala.util.chaining.scalaUtilChainingOps
import scala.scalajs.js
import org.scalajs.dom._

import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.webapp.Global
import fmgp.did.comm.EncryptedMessage

// @scala.scalajs.js.annotation.JSExportTopLevel("DIDClientHttp")
object Client {
  // @scala.scalajs.js.annotation.JSExport var tmp: Any = _

  // curl 'http://localhost:8080/db' -H "host: alice.did.fmgp.app"
  def getDB(url: String = "/db"): IO[DidFail, Option[MessageDB]] =
    Global.agent2Host(Global.agentVar.now()) match
      case None =>
        ZIO.succeed(None)
      case Some(value) =>
        val header = new Headers()
        header.append("x-forwarded-host", value)
        ZIO
          .fromPromiseJS(fetch(url, new RequestInit { method = HttpMethod.GET; headers = header }))
          .flatMap(e => ZIO.fromPromiseJS(e.text()))
          .catchAll(ex => ZIO.fail(SomeThrowable(ex)))
          .flatMap(_.fromJson[MessageDB] match
            case Left(error) => ZIO.fail(FailToParse(error))
            case Right(db)   => ZIO.succeed(Some(db))
          )

  def makeOps(
      data: String,
      url: String = "/ops"
  ): IO[SomeThrowable, String] = ZIO
    .fromPromiseJS(
      fetch(url, new RequestInit { method = HttpMethod.POST; body = data })
    )
    .flatMap(e => ZIO.fromPromiseJS(e.text()))
    .catchAll(ex => ZIO.fail(SomeThrowable(ex)))

  // def runGetDB: CancelableFuture[Option[MessageDB]] = {
  //   val program: IO[DidFail, Option[MessageDB]] = getDB()
  //   Unsafe.unsafe { implicit unsafe => // Run side efect
  //     Runtime.default.unsafe.runToFuture(
  //       program
  //         .catchAll(ex => ZIO.logError(ex.toString) *> ZIO.fail(new RuntimeException(ex.toString)))
  //     )
  //   }
  // }

  def makeDIDCommPost(
      data: EncryptedMessage,
      url: String
  ): IO[SomeThrowable, String] = {
    val request = new RequestInit {
      method = HttpMethod.POST
      headers = new Headers().tap(_.append("content-type", "application/didcomm-encrypted+json"))
      // headers = js.Array(js.Array("content-type", "application/didcomm-encrypted+json"))
      body = data.toJson
      // mode = RequestMode.`no-cors` // NOTE! this is make eveting not to work!
      cache = RequestCache.`no-cache`
    }

    ZIO
      .fromPromiseJS(fetch(url, request))
      .flatMap(e => ZIO.fromPromiseJS(e.text()))
      .catchAll(ex => ZIO.fail(SomeThrowable(ex)))
  }

}
