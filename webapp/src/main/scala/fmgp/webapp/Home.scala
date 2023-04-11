package fmgp.webapp

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import typings.std.stdStrings.text
import typings.mermaid

import fmgp.did._
object Home {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val statementVar = Var[Option[Statement]](initial = None)
  def onStatementUpdate(statement: Statement): Unit = statementVar.set(Some(statement))

  // #####################################

  def hack = mermaid.mod.default

  def apply(): HtmlElement = // rootElement
    div(
      p("Sandbox for DID Comm v2"),
      p("Navigate to ", b("OOB Tool "), MyRouter.navigateTo(MyRouter.OOBPage(App.oobExample))),
      p("Navigate to ", b("DID Resolver Tool "), MyRouter.navigateTo(MyRouter.ResolverPage(App.didExample))),
      p("Navigate to ", b("Encrypt Tool "), MyRouter.navigateTo(MyRouter.EncryptPage)),
      p("Navigate to ", b("Decrypt Tool "), MyRouter.navigateTo(MyRouter.DecryptPage)),
      p("Navigate to ", b("Basic Message "), MyRouter.navigateTo(MyRouter.BasicMessagePage)),
      p("Navigate to ", b("Trust Ping "), MyRouter.navigateTo(MyRouter.TrustPingPage)),
      p(
        "Navigate to ",
        b("TapIntoStream Tool "),
        MyRouter.navigateTo(MyRouter.TapIntoStreamPage),
        " (tap into all Alice's income messages)"
      ),
      p(
        "Navigate to ",
        b("Message DB"),
        " (only works for alice, bob and charlie)",
        MyRouter.navigateTo(MyRouter.AgentDBPage)
      ),
      p("Navigate to ", b("Documentation for scala-did lib"), MyRouter.navigateTo(MyRouter.DocPage)),
      br(),
      p("DIDs: "),
      div(child <-- statementVar.signal.map(e => getHtml(e)))
    )
  def getHtml(statement: Option[Statement], indent: Int = 0): ReactiveHtmlElement[HTMLElement] =
    div(className("mermaid"), statementToMermaid(statement), onMountCallback(ctx => { Global.update("div.mermaid") }))

  def statementToMermaid(s: Option[Statement]): String =
    AgentProvider.usersGraph

}
