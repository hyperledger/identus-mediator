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
import fmgp.did.resolver.peer.DIDPeer

object BasicMessageTool {

  val toDIDVar: Var[Option[DID]] = Var(initial = None)
  val plaintextMessageVar: Var[Either[String, PlaintextMessage]] =
    Var(initial = Left("Inicial State"))
  val encryptedMessageVar: Var[Option[EncryptedMessage]] = Var(initial = None)
  val inicialTextVar = Var(initial = "Hello, World!")
  def message = inicialTextVar.signal.map(e => BasicMessage(content = e))

  def jobPlaintextMessage(owner: Owner) =
    Signal
      .combine(
        Global.agentVar,
        toDIDVar,
        message
      )
      .map {
        case (mFrom, Some(to), msg) => Right(msg.toPlaintextMessage(mFrom.map(_.id), Set(to)))
        case (mFrom, None, msg)     => Left("Missing the 'TO'")
      }
      .map(plaintextMessageVar.set(_))
      .observe(owner)

  def job(owner: Owner) = Signal
    .combine(
      Global.agentVar,
      toDIDVar,
      message
    )
    .map {
      case (mFrom, None, msg) =>
        encryptedMessageVar.update(_ => None)
      case (None, Some(to), msg) =>
        val tmp = msg.toPlaintextMessage(None, Set(to))
        val programAux = OperationsClientRPC.anonEncrypt(tmp)
        val program = programAux.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(DidPeerResolver()))
          )
        }
      case (Some(from), Some(to), msg) =>
        val tmp = msg.toPlaintextMessage(Some(from.id), Set(to))
        val programAux = OperationsClientRPC.authEncrypt(tmp)
        val program = programAux.map(msg => encryptedMessageVar.update(_ => Some(msg)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideEnvironment(ZEnvironment(from, DidPeerResolver()))
          )
        }
    }
    .observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      jobPlaintextMessage(ctx.owner)
      job(ctx.owner)
      ()
    },
    code("BasicMessage Page"),
    p(
      overflowWrap.:=("anywhere"),
      "FROM: ",
      " ",
      code(child.text <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("none")))
    ),
    p(
      overflowWrap.:=("anywhere"),
      "TO: ",
      Global.makeSelectElementDID(toDIDVar),
      " ",
      code(child.text <-- toDIDVar.signal.map(_.map(_.string).getOrElse("none")))
    ),
    p("BasicMessage text:"),
    input(
      placeholder("Words"),
      `type`.:=("textbox"),
      autoFocus(true),
      value <-- inicialTextVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> inicialTextVar }
    ),
    p("Basic Message:"),
    pre(code(child.text <-- message.map(_.toString))),
    p("Plaintext Message:"),
    pre(code(child.text <-- plaintextMessageVar.signal.map(_.map(_.toJsonPretty).merge))),
    children <-- {
      plaintextMessageVar.signal
        .map(_.map(e => e.toJsonPretty))
        .map {
          case Left(error) => Seq.empty // new CommentNode("")
          case Right(json) =>
            Seq(
              button(
                "Copy to Encrypt Tool",
                onClick --> { _ => EncryptTool.dataTextVar.set(json) },
                MyRouter.navigateTo(MyRouter.EncryptPage)
              )
            )
        }
    }
  )

  def apply(): HtmlElement = rootElement
}
