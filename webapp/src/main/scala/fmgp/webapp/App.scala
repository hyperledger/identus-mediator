package fmgp.webapp

import scala.scalajs.js.annotation._

import org.scalajs.dom
import com.raquo.laminar.api.L._
import com.raquo.waypoint._

import MyRouter._
import com.raquo.airstream.ownership.ManualOwner

import fmgp.webapp.Home
import fmgp.did.DidExample
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
    .collectSignal[ResolverPage](page => ResolverTool(page))
    .collectStatic(EncryptPage)(EncryptTool())
    .collectStatic(DecryptPage)(DecryptTool())
    .collectStatic(BasicMessagePage)(BasicMessageTool())
    .collectStatic(TrustPingPage)(TrustPingTool())
    .collectStatic(TapIntoStreamPage)(TapIntoStreamTool())
    .collectStatic(DAppStorePage)(DAppStore())

  private val linkPages: List[Page] = List(
    HomePage,
    OOBPage(
      "eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOnZlcmlmaWVyIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ"
    ), // class
    DocPage,
    KeysPage,
    AgentDBPage,
    ResolverPage(
      "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
    ),
    EncryptPage,
    DecryptPage,
    BasicMessagePage,
    TrustPingPage,
    TapIntoStreamPage,
    DAppStorePage,
  )

}
