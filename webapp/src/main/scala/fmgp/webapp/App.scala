package fmgp.webapp

import scala.scalajs.js.annotation._

import org.scalajs.dom
import com.raquo.laminar.api.L._
import com.raquo.waypoint._

import MyRouter._
import com.raquo.airstream.ownership.ManualOwner

import fmgp.webapp.Home
object App {

  def main( /*args: Array[String]*/ ): Unit = {

    // This div, its id and contents are defined in index-fastopt.html and index-fullopt.html files
    lazy val container = dom.document.getElementById("app-container")

    lazy val appElement = {
      div(
        AppUtils.drawer(linkPages, MyRouter.router.$currentPage),
        AppUtils.drawerScrim,
        AppUtils.topBarHeader(MyRouter.router.$currentPage.map {
          case p: HomePage.type => "scala-did"
          case p                => p.title
        }),
        com.raquo.laminar.api.L.main(
          className("mdc-top-app-bar--fixed-adjust"),
          child <-- $selectedApp.$view
        )
      )
    }

    // Wait until the DOM is loaded, otherwise app-container element might not exist
    renderOnDomContentLoaded(container, appElement)
  }

  private val $selectedApp = SplitRender(MyRouter.router.$currentPage)
    .collectStatic(HomePage)(Home())
    .collectSignal[OOBPage](page => OutOfBandTool(page))
    .collectStatic(DocPage)(Doc())
    .collectStatic(KeysPage)(KeysHome())
    // .collectStatic(DIDPage)(DIDHome())
    .collectStatic(AgentDBPage)(AgentDB())
    .collectStatic(ResolverPage)(ResolverTool())
    .collectStatic(EncryptPage)(EncryptTool())
    .collectStatic(DecryptPage)(DecryptTool())
    .collectStatic(BasicMessagePage)(BasicMessageTool())
    .collectStatic(TrustPingPage)(TrustPingTool())
    .collectStatic(DAppStorePage)(DAppStore())

  private val linkPages: List[Page] = List(
    HomePage,
    OOBPage(
      "eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOnZlcmlmaWVyIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ"
    ), // class
    DocPage,
    KeysPage,
    // DIDPage,
    AgentDBPage,
    ResolverPage,
    EncryptPage,
    DecryptPage,
    BasicMessagePage,
    TrustPingPage,
    DAppStorePage,
  )

}
