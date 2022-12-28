package fmgp.webapp

import org.scalajs.dom
import com.raquo.laminar.api.L._
import typings.std.stdStrings.text

import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLElement

import typings.mermaid
import fmgp.did.example.AgentProvider

object Home {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val statementVar = Var[Option[Statement]](initial = None)
  def onStatementUpdate(statement: Statement): Unit = statementVar.set(Some(statement))

  // #####################################

  def hack = mermaid.mod.default

  def apply(): HtmlElement = // rootElement
    div(
      p("DID Comm examples and tooling"),
      p("Navigate to ", b("DID Resolver Tool "), MyRouter.navigateTo(MyRouter.ResolverPage)),
      p("Navigate to ", b("Decrypt Tool "), MyRouter.navigateTo(MyRouter.DecryptPage)),
      p("Navigate to ", b("Basic Message "), MyRouter.navigateTo(MyRouter.BasicMessagePage)),
      br(),
      p("DIDs: "),
      div(child <-- statementVar.signal.map(e => getHtml(e)))
    )
  def getHtml(statement: Option[Statement], indent: Int = 0): ReactiveHtmlElement[HTMLElement] =
    div(className("mermaid"), statementToMermaid(statement), onMountCallback(ctx => { update }))

  def statementToMermaid(s: Option[Statement]): String =
    AgentProvider.usersGraph

  def update = {
    println("MermaidApp Update!!")
    // val config = mermaid.mermaidAPIMod.mermaidAPI.Config().setStartOnLoad(false)
    // mermaid.mod.default.initialize(config)
    mermaid.mod.default.init("div.mermaid")
  }
}
