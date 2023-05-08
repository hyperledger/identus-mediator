package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto._
import com.raquo.laminar.CollectionCommand

object AgentKeys {

  def keyStoreVar = Global.agentVar.signal.map {
    case None        => KeyStore(Set.empty)
    case Some(agent) => agent.keyStore
  }
  def childrenSignal: Signal[Seq[Node]] = keyStoreVar.map(_.keys.toSeq.map(_.toJson).map(code(_)))

  val keyStore2Var: Var[KeyStore] = Var(initial = KeyStore(Set.empty))

  private val commandObserver = Observer[String] { case str =>
    str.fromJson[PrivateKey] match
      case Left(error)   => dom.window.alert(s"Fail to parse key: $error")
      case Right(newKey) => keyStore2Var.update(ks => ks.copy(ks.keys + newKey))
  }

  val rootElement = div(
    /*
    code("Keys Page"),
    div(
      div(
        p(
          "You can use folowing website to ",
          a(href := "https://8gwifi.org/jwkfunctions.jsp", target := "_blank", "Generate test Keys"),
          ": X25519; Ed25519; P-256; P-384; P-521; P-256K (secp256k1)",
        ),
        p(
          "You can also use this website to ",
          a(href := "https://mkjwk.org/", target := "_blank", "Generate test Keys"),
        )
      ),
      div(
        input(
          placeholder("Add new key (JWT format)"),
          autoFocus(true),
          inContext { thisNode =>
            // Note: mapTo below accepts parameter by-name, evaluating it on every enter key press
            AppUtils.onEnterPress.mapTo(thisNode.ref.value).filter(_.nonEmpty) -->
              commandObserver.contramap[String] { text =>
                thisNode.ref.value = "" // clear input
                text
              }
          }
        )
      )
    ),
    div(
      div(child.text <-- keyStoreVar.map(_.keys.size).map(c => s"KeyStore (with $c keys):")),
      div(children <-- childrenSignal)
    ),
     */
    table(
      tr(th("type"), th("isPointOnCurve"), th("Keys Id")),
      children <-- keyStoreVar.map(
        _.keys.toSeq
          .map { key =>
            key match
              case k @ OKPPrivateKey(kty, crv, d, x, kid) =>
                tr(
                  td(code(kty.toString)),
                  td(code("N/A")),
                  td(code(kid.getOrElse("missing"))),
                )
              case k @ ECPrivateKey(kty, crv, d, x, y, kid) =>
                tr(
                  td(code(kty.toString)),
                  td(code(k.isPointOnCurve)),
                  td(code(kid.getOrElse("missing"))),
                )

          }
      ),
    ),
  )
  def apply(): HtmlElement = rootElement
}
