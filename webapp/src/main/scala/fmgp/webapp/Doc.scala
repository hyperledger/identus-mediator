package fmgp.webapp

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import typings.std.stdStrings.text
import typings.mermaid

import fmgp.did._

import laika.api._
import laika.format._
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting

import org.scalajs.dom.{DOMParser, MIMEType}

object Doc {

  val transformer = Transformer
    .from(Markdown)
    .to(HTML)
    .using(GitHubFlavor, SyntaxHighlighting)
    .build
  val htmlRenderer = Renderer.of(HTML).build

  val result = transformer.transform(fmgp.did.DocSource.readme) match
    case Left(value)  => value.message
    case Right(value) => value

  val divContainer = {
    val tmpDiv = div()
    // val html = DOMParser().parseFromString(result, MIMEType.`text/html`)
    tmpDiv.ref.innerHTML = result
    tmpDiv
  }

  val rootElement = {
    div(
      p("DID Comm Documentation"),
      divContainer,
    )
  }
  // innerHTML

  def apply(): HtmlElement = rootElement
}
