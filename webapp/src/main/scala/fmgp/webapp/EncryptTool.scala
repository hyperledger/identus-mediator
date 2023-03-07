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
import fmgp.did.resolver.uniresolver.Uniresolver
import fmgp.crypto.error.DidFail
import com.raquo.airstream.core.Sink
import fmgp.did.comm.protocol.routing2.ForwardMessage

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

  val encryptedMessageVar: Var[Option[Either[DidFail, (PlaintextMessage, EncryptedMessage)]]] = Var(initial = None)
  val dataTextVar = Var(initial = example.toJsonPretty)
  val curlCommandVar: Var[Option[String]] = Var(initial = None)
  val outputFromCallVar = Var[Option[EncryptedMessage]](initial = None)
  val forwardMessageVar = Var[Option[ForwardMessage]](initial = None)

  def plaintextMessage = dataTextVar.signal.map(_.fromJson[PlaintextMessage])

  def jobNextForward(owner: Owner) = {
    def program(pMsg: PlaintextMessage, eMsg: EncryptedMessage): ZIO[Any, DidFail, Option[ForwardMessage]] = {
      pMsg.to.flatMap(_.headOption) match
        case None => ZIO.none
        case Some(originalTO) =>
          for {
            resolver <- ZIO.service[Resolver]
            doc <- resolver.didDocument(originalTO)
            mMediatorDid = doc.getDIDServiceDIDCommMessaging.headOption.toSeq
              .flatMap(_.getServiceEndpointNextForward)
              .headOption
            forwardMessage = mMediatorDid.flatMap(mediatorDid =>
              ForwardMessage
                .buildForwardMessage(
                  to = Set(mediatorDid.asTO),
                  next = originalTO.asDIDURL.toDID,
                  msg = eMsg,
                )
                .toOption
            )
          } yield forwardMessage
    }.provide(ZLayer.succeed(DidPeerResolver.default))
    // .provide(MultiResolver(DidPeerResolver.default, Uniresolver.default))

    encryptedMessageVar.signal
      .map {
        case Some(Right((pMsg, eMsg))) =>
          Unsafe.unsafe { implicit unsafe =>
            Runtime.default.unsafe.fork(
              program(pMsg: PlaintextMessage, eMsg: EncryptedMessage)
                .map(forwardMessageVar.set(_))
            )
          } // Run side efect
        case _ => None
      }
      .observe(owner)
  }

  def calEncryptedViaRPC(owner: Owner) = Signal
    .combine(
      Global.agentVar,
      plaintextMessage
    )
    .map {
      case (_, Left(_)) =>
        encryptedMessageVar.update(_ => None)
      case (None, Right(pMsg)) =>
        val program = OperationsClientRPC
          .anonEncrypt(pMsg)
          .either
          .map(_.map((pMsg, _)))
          .map(e => encryptedMessageVar.update(_ => Some(e)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(DidPeerResolver()))
          )
        }
      case (Some(agent), Right(pMsg)) =>
        val program = OperationsClientRPC
          .authEncrypt(pMsg)
          .either
          .map(_.map((pMsg, _)))
          .map(e => encryptedMessageVar.update(_ => Some(e)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(agent, DidPeerResolver()))
          )
        }
    }
    .observe(owner)

  def curlCommand(owner: Owner) = encryptedMessageVar.signal
    .map(_.flatMap(_.toOption))
    .map(_.flatMap { (_, eMsg) =>
      DIDPeer2
        .fromDID(eMsg.recipientsSubject.head)
        .toOption
        .flatMap(_.document.getDIDServiceDIDCommMessaging.headOption)
        .flatMap(_.getServiceEndpointAsURIs.headOption)
        .map { uri =>
          s"""curl -X POST $uri -H 'content-type: application/didcomm-encrypted+json' -d '${eMsg.toJson}'"""
        }
    })
    .map(e => curlCommandVar.set(e))
    .observe(owner)

  def curlProgram(msg: EncryptedMessage) = {
    DIDPeer2
      .fromDID(msg.recipientsSubject.head)
      .toOption
      .flatMap(_.document.getDIDServiceDIDCommMessaging.headOption)
      .flatMap(_.getServiceEndpointAsURIs.headOption)
      .map { uri =>
        Client
          .makeDIDCommPost(msg, uri)
          .map(_.fromJson[EncryptedMessage])
          .map {
            case Left(value)  => outputFromCallVar.set(None)
            case Right(value) => outputFromCallVar.set(Some(value))
          }
      }
      .foreach { program =>
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program
          )
        }
      }

  }

  val rootElement = div(
    onMountCallback { ctx =>
      calEncryptedViaRPC(ctx.owner) // side effect
      jobNextForward(ctx.owner) // side effect
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
    div(child <-- plaintextMessage.map {
      case Left(error) => pre(code(""))
      case Right(msg) =>
        msg.return_route match
          case None =>
            div(
              pre(code("return_route is undefined (default)")),
              button(
                """Add "return_route":"all"""",
                onClick --> { _ =>
                  dataTextVar.set(
                    msg
                      .asInstanceOf[PlaintextMessageClass] // FIXME
                      .copy(return_route = Some(ReturnRoute.all))
                      .toJsonPretty
                  )
                }
              ),
            )
          case Some(value) => new CommentNode("")
    }),
    p("Plaintext Message (Or error report):"),
    pre(code(child.text <-- plaintextMessage.map {
      case Right(msg)  => msg.toJsonPretty
      case Left(error) => s"Error: $error"
    })),
    p(
      "Encrypted Message",
      "(NOTE: This is executed as a RPC call to the JVM server, since the JS version has not yet been fully implemented)"
    ),
    child <-- encryptedMessageVar.signal.map {
      case None                   => "None"
      case Some(Left(error))      => "Error when encrypting " + error.toJsonPretty
      case Some(Right((_, eMsg))) => pre(code(eMsg.toJsonPretty))

    },
    child <-- forwardMessageVar.signal.map {
      case None => "No ForwardMessage"
      case Some(forwardMsg) =>
        forwardMsg.toPlaintextMessage(None) match
          case Left(error) => s"Error on ForwardMessage $error"
          case Right(pMsg) =>
            div(
              button(
                "Copy ForwardMessage into textbox",
                onClick --> { _ => dataTextVar.set(pMsg.toJsonPretty) }
              ),
              pre(code(pMsg.toJsonPretty))
            )
    },
    button(
      "Copy EncryptedMessage to clipboard",
      onClick --> Global.clipboardSideEffect(
        encryptedMessageVar.now() match
          case None                   => "None"
          case Some(Left(error))      => "Error when encrypting " + error.toJson
          case Some(Right((_, eMsg))) => eMsg.toJson
      )
    ),
    p(code(child.text <-- curlCommandVar.signal.map(_.getOrElse("curl")))),
    div(
      child <-- curlCommandVar.signal
        .map {
          case Some(curlStr) =>
            div(
              button("Copy to curl", onClick --> Global.clipboardSideEffect(curlStr)),
              button(
                "Make HTTP POST",
                onClick --> Sink.jsCallbackToSink(_ =>
                  encryptedMessageVar.now() match {
                    case Some(Right((_, eMsg))) => curlProgram(eMsg)
                    case _                      => // None
                  }
                )
              )
            )
          case None => div("Valid message")
        }
    ),
    div(
      child <-- outputFromCallVar.signal.map {
        case None => new CommentNode("")
        case Some(reply) =>
          div(
            p("Output of the HTTP Call"),
            pre(code(reply.toJsonPretty)),
            button(
              "Copy reply to Decryot Tool",
              onClick --> { _ => DecryptTool.dataVar.set(reply.toJsonPretty) },
              MyRouter.navigateTo(MyRouter.DecryptPage)
            )
          )
      }
    ),
  )

  def apply(): HtmlElement = rootElement
}
