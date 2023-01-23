package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.extension._
import fmgp.did.resolver.peer.DIDPeer2
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.crypto.error.DidFail

object EncryptTool {
  def example = PlaintextMessageClass(
    id = MsgID(),
    `type` = PIURI("basic"),
    to = Some(Set(Global.agentVar.now().flatMap(o => TO.either(o.id.string).toOption).getOrElse(TO("did:TO:123")))),
    from = Some(FROM("did:FROM:123")),
    thid = Some(MsgID()),
    created_time = Some(123456789),
    expires_time = Some(123456789),
    body = JSON_RFC7159(),
    attachments = Some(Seq.empty[Attachment]),
    // # Extensions
    `accept-lang` = Some(Seq("PT")),
    lang = Some("PT"), // IANA’s language codes  // IANA’s language subtag registry.
    // l10n = Some(L10n(
    //   inline = Some(Seq[L10nInline),
    //   service = Some(L10nService),
    //   table = Some(L10nTable)
    //   )),
    // sender_order: NotRequired[SenderOrder] = None,
    // sent_count: NotRequired[SentCount] = None,
    // received_orders: NotRequired[Seq[ReceivedOrdersElement]] = None,
  )

  val encryptedMessageVar: Var[Option[Either[DidFail, EncryptedMessage]]] = Var(initial = None)
  val dataTextVar = Var(initial = example.toJsonPretty)
  val curlCommandVar: Var[Option[String]] = Var(initial = None)

  def plaintextMessage = dataTextVar.signal.map(_.fromJson[PlaintextMessage])

  def calEncryptedViaRPC(owner: Owner) = Signal
    .combine(
      Global.agentVar,
      plaintextMessage
    )
    .map {
      case (_, Left(_)) =>
        encryptedMessageVar.update(_ => None)
      case (None, Right(msg)) =>
        val programAux = OperationsClientRPC.anonEncrypt(msg)
        val program = programAux.either.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(DidPeerResolver))
          )
        }
      case (Some(agent), Right(msg)) =>
        val programAux = OperationsClientRPC.authEncrypt(msg)
        val program = programAux.either.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(agent, DidPeerResolver))
          )
        }
    }
    .observe(owner)

  def curlCommand(owner: Owner) = encryptedMessageVar.signal
    .map(_.flatMap(_.toOption))
    .map(_.flatMap { em =>
      DIDPeer2
        .fromDID(em.recipientsSubject.head)
        .toOption
        .flatMap(_.document.getDIDServiceDIDCommMessaging.headOption)
        .flatMap(_.getServiceEndpointAsURIs.headOption)
        .map { uri =>
          s"""curl -X POST $uri -H 'content-type: application/didcomm-encrypted+json' -d '${em.toJson}'"""
        }
    })
    .map(e => curlCommandVar.set(e))
    .observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      calEncryptedViaRPC(ctx.owner) // side effect
      curlCommand(ctx.owner) // side effect
      ()
    },
    code("DecryptTool Page"),
    p(
      overflowWrap.:=("anywhere"),
      "Agent: ",
      " ",
      code(child.text <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("none")))
    ),
    p(
      "Plaintext Text:",
      button(
        "RESET",
        onClick --> Observer(_ => dataTextVar.set(example.toJsonPretty))
      )
    ),
    textArea(
      rows := 20,
      cols := 80,
      autoFocus(true),
      value <-- dataTextVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> dataTextVar }
    ),
    p("Plaintext Message (Or rrror report):"),
    pre(code(child.text <-- plaintextMessage.map {
      case Right(msg)  => msg.toJsonPretty
      case Left(error) => s"Error: $error"
    })),
    p(
      "Encrypted Message",
      "(NOTE: This is executed as a RPC call to the JVM server, since the JS version has not yet been fully implemented)"
    ),
    pre(
      code(
        child.text <-- encryptedMessageVar.signal.map {
          case None              => "None"
          case Some(Left(error)) => "Error when encrypting " + error.toJsonPretty
          case Some(Right(eMsg)) => eMsg.toJsonPretty
        }
      )
    ),
    button(
      "Copy to clipboard",
      onClick --> Global.clipboardSideEffect(
        encryptedMessageVar.now() match
          case None              => "None"
          case Some(Left(error)) => "Error when encrypting " + error.toJson
          case Some(Right(eMsg)) => eMsg.toJson
      )
    ),
    p(code(child.text <-- curlCommandVar.signal.map(_.getOrElse("curl")))),
    div(
      child <-- curlCommandVar.signal
        .map { // .map(curlStr => button("Copy to curl", onClick --> Global.clipboardSideEffect(curlStr)))
          case Some(curlStr) => button("Copy to curl", onClick --> Global.clipboardSideEffect(curlStr))
          case None          => div("No curl")
        }
    ),
  )

  def apply(): HtmlElement = rootElement
}
