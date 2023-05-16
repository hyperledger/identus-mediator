package fmgp.webapp

import zio._
import zio.json._
import com.raquo.laminar.api.L._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.method.peer._

object BasicMessageTool {

  val toDIDVar: Var[Option[DID]] = Var(initial = None)
  val plaintextMessageVar: Var[Either[String, PlaintextMessage]] = Var(initial = Left("Inicial State"))
  val inicialTextVar = Var(initial = "Hello, World!")
  def message = Signal
    .combine(
      Global.agentVar,
      toDIDVar,
      inicialTextVar
    )
    .map {
      case (mFrom, Some(to), msgStr) => Right(BasicMessage(to = Set(to), from = mFrom.map(_.id), content = msgStr))
      case (mFrom, None, msgSTR)     => Left("Missing the 'TO'")
    }

  def jobPlaintextMessage(owner: Owner) = message
    .map(_.map(_.toPlaintextMessage))
    .map(plaintextMessageVar.set(_))
    .observe(owner)

  val rootElement = div(
    onMountCallback(ctx => { jobPlaintextMessage(ctx.owner); () }),
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
