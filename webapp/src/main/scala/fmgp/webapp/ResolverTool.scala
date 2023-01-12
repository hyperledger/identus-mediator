package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import com.raquo.airstream.ownership._
import fmgp.crypto.error._
import fmgp.did.resolver.peer.DIDPeer

object ResolverTool {

  val didVar: Var[Option[DID]] = Var(initial = None)
  val customVar: Var[String] = Var(initial = "")
  val didDocumentVar: Var[Either[String, DIDDocument]] = Var(initial = Left(""))

  val job = Signal
    .combine(didVar, customVar)
    .map {
      case (Some(did), custom) =>
        DIDPeer.fromDID(did) match
          case Left(error) => didDocumentVar.update(_ => Left(error))
          case Right(peer) => didDocumentVar.update(_ => Right(peer.document))
      case (None, custom) =>
        val program = {
          ZIO
            .fromEither(FROMTO.either(custom))
            .flatMap(did => DidPeerResolver.didDocument(did))
            .mapBoth(
              errorInfo => didDocumentVar.update(_ => Left(errorInfo.toString)),
              doc => didDocumentVar.update(_ => Right(doc))
            )
        }
        Unsafe.unsafe { implicit unsafe => Runtime.default.unsafe.fork(program) } // Run side efect
    }
    .observe(App.owner)

  val rootElement = div(
    code("DID Resolver Page (only 'did:peer' is supported)"),
    p(
      "Agent: ",
      Global.makeSelectElementDID(didVar),
      " ",
      code(child.text <-- didVar.signal.map(_.map(_.string).getOrElse("custom")))
    ),
    div(child <-- didVar.signal.map {
      case Some(agent) => div()
      case None =>
        div(
          p("Input the custom did"),
          input(
            placeholder("did:peer:..."),
            `type`.:=("textbox"),
            autoFocus(true),
            value <-- customVar,
            inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> customVar }
          ),
        )
    }),
    pre(
      code(
        child.text <-- didDocumentVar.signal.map {
          case Right(doc)      => doc.toJsonPretty
          case Left(errorInfo) => errorInfo
        }
      )
    ),
    button(
      "Copy to clipboard",
      onClick --> Global.clipboardSideEffect(
        didDocumentVar.now() match
          case Right(doc)      => doc.toJson
          case Left(errorInfo) => errorInfo
      )
    )
  )

  def apply(): HtmlElement = rootElement
}
