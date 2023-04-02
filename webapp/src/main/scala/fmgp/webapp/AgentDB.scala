package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._
import com.raquo.airstream.core._

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.crypto.error._

import fmgp.did.AgentProvider
object AgentDB {
  val dbVar: Var[Option[MessageDB]] = Var(initial = None)

  val scope: HtmlProp[String] = htmlProp("scope", StringAsIsCodec)

  def emSignal = {
    def projection(
        key: HashEncryptedMessage,
        input: (HashEncryptedMessage, EncryptedMessage),
        signal: Signal[(HashEncryptedMessage, EncryptedMessage)]
    ) = tr(
      className("mdc-data-table__row"),
      th(className("mdc-data-table__cell mdc-data-table__cell--numeric"), scope := "row", key),
      th(className("mdc-data-table__cell mdc-data-table__cell--numeric"), scope := "row", input.hashCode),
      td(className("mdc-data-table__cell"), input._2.`protected`.obj.alg.toString),
      td(className("mdc-data-table__cell"), input._2.`protected`.obj.enc.toString),
      td(className("mdc-data-table__cell"), input._2.`protected`.obj.epk.crv.toString),
    )
    dbVar.signal.map(_.toSeq.flatMap(_.db.toSeq)).split(_._1)(projection _)
  }

  def updateDB =
    val program: IO[DidFail, Option[MessageDB]] = Client.getDB()
    Unsafe.unsafe { implicit unsafe => // Run side efect
      Runtime.default.unsafe.fork(
        program
          .map(db => dbVar.update(_ => db))
          .tapError(ex => ZIO.logError(ex.toString))
      )
    }

  def jobUpdateFromAgentSignal(owner: Owner) =
    Global.agentVar.signal.map(e => updateDB).observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      jobUpdateFromAgentSignal(ctx.owner)
      updateDB
      ()
    },
    code("AgentDB Page"),
    p(
      overflowWrap.:=("anywhere"),
      "Agent: ",
      " ",
      code(child.text <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("NO AGENT IS SELECTED!")))
    ),
    div(
      AppUtils.myButton("Refresh NOW").amend(onClick --> Observer(_ => updateDB)),
    ),
    // div(child <-- dbVar.signal.map(_.toJsonPretty)),
    div(
      className("mdc-data-table"),
      div(
        className("mdc-data-table__table-container"),
        table(
          className("mdc-data-table__table"),
          aria.label := "Dessert calories",
          thead(
            tr(
              className("mdc-data-table__header-row"),
              th(
                className("mdc-data-table__header-cell mdc-data-table__header-cell--numeric"),
                role := "columnheader",
                scope := "col",
                "key"
              ),
              th(
                className("mdc-data-table__header-cell mdc-data-table__header-cell--numeric"),
                role := "columnheader",
                scope := "col",
                "hash"
              ),
              th(className("mdc-data-table__header-cell "), role := "columnheader", scope := "col", "alg"),
              th(className("mdc-data-table__header-cell"), role := "columnheader", scope := "col", "enc"),
              th(className("mdc-data-table__header-cell"), role := "columnheader", scope := "col", "crv"),
            )
          ),
          tbody(
            className("mdc-data-table__content"),
            children <-- emSignal
          )
        )
      )
    )
  )

  def apply(): HtmlElement = rootElement
}
