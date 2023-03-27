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
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.crypto.error._

import fmgp.did.AgentProvider
object DecryptTool {
  val dataVar: Var[String] = Var(initial = "")
  val encryptedMessageVar: Signal[Either[String, EncryptedMessage]] =
    dataVar.signal.map(_.fromJson[EncryptedMessage])
  val decryptDataVar: Var[Option[Array[Byte]]] = Var(initial = None)
  val decryptMessageVar: Var[Option[Either[DidFail, Message]]] = Var(initial = None)

  def job(owner: Owner) =
    Signal
      .combine(
        Global.agentVar,
        encryptedMessageVar
      )
      .map {
        case (None, _) =>
          decryptMessageVar.set(None) // side effect!
        case (Some(agent), Left(error)) =>
          decryptMessageVar.set(Some(Left(FailToParse("Fail to parse Encrypted Message: " + error)))) // side effect!
        case (Some(agent), Right(msg)) =>
          val program = {
            msg.`protected`.obj match
              case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
                OperationsClientRPC
                  .anonDecryptRaw(msg)
                  .flatMap { data =>
                    decryptDataVar.set(Some(data)) // side effect!
                    Operations.parseMessage(data)
                  }
              case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
                OperationsClientRPC
                  .authDecryptRaw(msg)
                  .flatMap { data =>
                    decryptDataVar.set(Some(data)) // side effect!
                    Operations.parseMessage(data)
                  }
          }.mapBoth(
            error => {
              decryptDataVar.set(None) // side effect!
              decryptMessageVar.set(Some(Left(error))) // side effect!
            },
            msg => decryptMessageVar.set(Some(Right(msg))) // side effect!
          )
          Unsafe.unsafe { implicit unsafe => // Run side efect
            Runtime.default.unsafe.fork(
              program.provideEnvironment(ZEnvironment(agent, DidPeerResolver()))
            )
          }
      }
      .observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      job(ctx.owner)
      ()
    },
    code("DecryptTool Page"),
    p(
      overflowWrap.:=("anywhere"),
      "Agent: ",
      " ",
      code(child.text <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("NO AGENT IS SELECTED!")))
    ),
    p("Encrypted Message Data:"),
    textArea(
      rows := 20,
      cols := 80,
      autoFocus(true),
      placeholder("<EncryptedMessage>"),
      value <-- dataVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> dataVar }
    ),
    p("Encrypted Message Protected Header (parsed):"),
    div(
      children <-- encryptedMessageVar.map { mMsg =>
        mMsg.toSeq
          .map(_.`protected`.obj.toJsonPretty)
          .map(p(_))
      },
    ),
    p("Recipients:"),
    div(
      children <-- encryptedMessageVar.map { mMsg =>
        mMsg.toSeq
          .flatMap(_.recipients.toSeq.map(_.header.kid.value))
          .map(e => p(s" - kid: $e"))
      },
    ),
    p("Raw Data (as UTF8) after decrypting:"),
    pre(code(child.text <-- decryptDataVar.signal.map {
      case None        => "<none>"
      case Some(bytes) => String(bytes)
    })),
    p("Message after decrypting:"),
    pre(code(child.text <-- decryptMessageVar.signal.map {
      case None                => "<none>"
      case Some(Left(didFail)) => didFail.toString
      case Some(Right(msg))    => msg.toJsonPretty
    })),
    button(
      "Copy to clipboard",
      onClick --> Global.clipboardSideEffect(
        decryptMessageVar.now() match
          case None                => "None"
          case Some(Left(didFail)) => didFail.toString
          case Some(Right(msg))    => msg.toJson
      )
    )
  )

  def apply(): HtmlElement = rootElement
}
