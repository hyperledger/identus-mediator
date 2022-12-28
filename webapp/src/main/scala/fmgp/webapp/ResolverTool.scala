package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.example._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import com.raquo.airstream.ownership._
import fmgp.crypto.error._

object ResolverTool {

  val agentVar: Var[Option[AgentDIDPeer]] = Var(initial = None)
  val customVar: Var[String] = Var(initial = "")
  val didDocumentVar: Var[Either[String, DIDDocument]] = Var(initial = Left(""))

  val job = Signal
    .combine(agentVar, customVar)
    .map {
      case (Some(agent), custom) => didDocumentVar.update(_ => Right(agent.id.document))
      case (None, custom) =>
        val program = {
          ZIO
            .fromEither(DIDSubject.either(custom))
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
    code("DID Resolver Page (for 'did:peer')"),
    p(
      "Agent: ",
      select(
        value <-- agentVar.signal.map(Global.getAgentName(_)),
        onChange.mapToValue.map(e => AgentProvider.allAgents.get(e)) --> agentVar,
        Global.dids.map { step => option(value := step, step) }
      )
    ),
    pre(code(child.text <-- agentVar.signal.map(_.map(_.id.string).getOrElse("custom")))),
    div(child <-- agentVar.signal.map {
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
    )
  )

  def apply(): HtmlElement = rootElement
}
