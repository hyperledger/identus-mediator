package fmgp.webapp

import org.scalajs.dom
import com.raquo.laminar.api.L._
import typings.std.stdStrings.text

import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLElement

import typings.mermaid
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("DAppStore")
object DAppStore {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  @JSExport
  def xxx = {
    IpfsClient.dagGet().map {
      case Left(l)  => "aaa"
      case Right(r) => onStatementUpdate(r)
    }
  }

  val statementVar = Var[Option[Statement]](initial = None)
  def onStatementUpdate(statement: Statement): Unit = statementVar.set(Some(statement))

  // #####################################

  @JSExport
  def hack = mermaid.mod.default

  def apply(): HtmlElement = // rootElement
    div(child <-- statementVar.signal.map(e => getHtml(e)))

  def getHtml(statement: Option[Statement], indent: Int = 0): ReactiveHtmlElement[HTMLElement] =
    div(className("mermaid"), statementToMermaid(statement), onMountCallback(ctx => { update }))

  def statementToMermaid(s: Option[Statement]): String = {

    // LR
    val aux = s"""
    |flowchart TD
    |  DID_1 %%(did:prism:e6dfb26d195076c6408ae479eae6a128ab6f859f0d1c296f602b3dba6a3e714b)
    |  DID_2 %%(did:prism:e6dfb26d195076c6408ae479eae6a128ab6f859f0d1c296f602b3dba6a3e714c)
    |  CID_1[[CID: QmX6CvErxHkuybopMsRYC3oHZvsrkbLQpYkfzCvP8nNFfi]]
    |  subgraph Statements
    |    DID_1 -->|UPTODATE| CID_1
    |    DID_1 ==>|NOT SECURE| CID_1
    |    DID_2 ==>|SECURE| CID_1
    |  end
    |
    |  linkStyle 0 stroke-width:2px,fill:none,stroke:yellow;
    |  linkStyle 1 stroke-width:3px,fill:none,stroke:red;
    |  linkStyle 2 stroke-width:3px,fill:none,stroke:grean;
    |
    |  click A1 call callback() "Tooltip"
    |  click CID_1 href "https://ipfs.io/ipfs/QmQesMFhs7TU3xkuQrkkKAxfqxfdnBQuq62DnqVAcHNAN5" "Open ipfs/QmQesMFhs7TU3xkuQrkkKAxfqxfdnBQuq62DnqVAcHNAN5"
    |${s.getOrElse("")}
    """.stripMargin
    println(aux)
    aux
  }

  @JSExport
  def update = {
    println("MermaidApp Update!!")
    // val config = mermaid.mermaidAPIMod.mermaidAPI.Config().setStartOnLoad(false)
    // mermaid.mod.default.initialize(config)
    mermaid.mod.default.init("div.mermaid")
  }
}
