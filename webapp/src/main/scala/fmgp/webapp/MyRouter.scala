package fmgp.webapp

import com.raquo.laminar.api.L.{_, given}
import com.raquo.waypoint._
import org.scalajs.dom
import upickle.default._

object MyRouter {
  sealed abstract class Page(
      val title: String,
      val icon: String // https://fonts.google.com/icons?selected=Material+Icons+Outlined
  )

  case object HomePage extends Page("Home", "home")
  case object KeysPage extends Page("Keys", "key")
  // case object DIDPage extends Page("DID", "visibility")
  case object DecryptPage extends Page("Decrypt", "email")
  case object BasicMessagePage extends Page("BasicMessage", "message")

  case object DAppStorePage extends Page("DAppStore", "share")

  given HomePageRW: ReadWriter[HomePage.type] = macroRW

  given rw: ReadWriter[Page] = macroRW

  private val routes = List(
    Route.static(HomePage, root / endOfSegments, Router.localFragmentBasePath),
    Route.static(KeysPage, root / "keys" / endOfSegments, Router.localFragmentBasePath),
    // Route.static(DIDPage, root / "did" / endOfSegments, Router.localFragmentBasePath),
    Route.static(DecryptPage, root / "didcomm" / endOfSegments, Router.localFragmentBasePath),
    Route.static(DecryptPage, root / "decrypt" / endOfSegments, Router.localFragmentBasePath),
    Route.static(BasicMessagePage, root / "basicmessage" / endOfSegments, Router.localFragmentBasePath),
    Route.static(DAppStorePage, root / "dapp" / endOfSegments, Router.localFragmentBasePath),
  )

  val router = new Router[Page](
    routes = routes,
    getPageTitle = _.title, // displayed in the browser tab next to favicon
    serializePage = page => write(page)(rw), // serialize page data for storage in History API log
    deserializePage = pageStr => read(pageStr)(rw), // deserialize the above
    routeFallback = { (_: String) => HomePage },
  )(
    $popStateEvent = windowEvents.onPopState, // this is how Waypoint avoids an explicit dependency on Laminar
    owner = unsafeWindowOwner // this router will live as long as the window
  )

  // Note: for fragment ('#') URLs this isn't actually needed.
  // See https://github.com/raquo/Waypoint docs for why this modifier is useful in general.
  def navigateTo(page: Page): Binder[HtmlElement] = Binder { el =>

    val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

    if (isLinkElement) {
      el.amend(href(router.absoluteUrlForPage(page)))
    }

    // If element is a link and user is holding a modifier while clicking:
    //  - Do nothing, browser will open the URL in new tab / window / etc. depending on the modifier key
    // Otherwise:
    //  - Perform regular pushState transition
    (onClick
      .filter(ev => !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey)))
      .preventDefault
      --> (_ => router.pushState(page))).bind(el)
  }
}
