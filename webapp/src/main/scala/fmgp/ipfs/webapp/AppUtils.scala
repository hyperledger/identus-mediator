package fmgp.ipfs.webapp

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport

import org.scalajs.dom
import com.raquo.laminar.api.L._
import com.raquo.domtypes.generic.codecs._

import MyRouter._

@JSExportTopLevel("AppUtils")
object AppUtils {

  val menuClickObserver = Observer[dom.MouseEvent](onNext = ev => {
    import typings.materialDrawer.mod.MDCDrawer
    val tmp = MDCDrawer.attachTo(dom.window.document.querySelector(".mdc-drawer"))
    tmp.open_=(!tmp.open)
  })

  val optionsClickObserver = Observer[dom.MouseEvent](onNext = ev => {
    import typings.materialMenu.mod.MDCMenu
    val tmp = MDCMenu.attachTo(dom.window.document.querySelector(".mdc-menu"))
    tmp.open_=(!tmp.open)
  })

  def topBarHeader(title: Signal[String]) = { // (title: String) = {
    val menuButton = button(
      className("material-icons mdc-top-app-bar__navigation-icon mdc-icon-button"),
      aria.label("Options"),
      onClick --> menuClickObserver,
      "menu"
    )
    typings.materialRipple.mod.MDCRipple.attachTo(menuButton.ref)

    val options = {
      div(
        className("mdc-menu mdc-menu-surface"),
        minWidth("200px"),
        ul(
          className("mdc-list"),
          role("menu"),
          aria.hidden(true),
          aria.orientation("vertical"),
          tabIndex(-1),
          "WorldExamplesOption.Clean.makeLi",
          li(className("mdc-list-divider"), role("separator")),
          li(
            ul(
              className("mdc-menu__selection-group"),
              // WorldExamplesOption.values.filterNot(_ == WorldExamplesOption.Clean).toSeq.map(_.makeLi),
            )
          ),
        )
      )
    }

    val optionsButton = button(
      className("material-icons mdc-top-app-bar__navigation-icon mdc-icon-button"),
      aria.label("Open navigation menu"),
      onClick --> optionsClickObserver,
      "more_vert"
    )
    typings.materialMenu.mod.MDCMenu.attachTo(options.ref)

    header(
      className("mdc-top-app-bar"),
      div(
        className("mdc-top-app-bar__row"),
        section(
          className("mdc-top-app-bar__section mdc-top-app-bar__section--align-start"),
          menuButton,
          span(className("mdc-top-app-bar__title"), child.text <-- title)
        ),
        section(
          className("mdc-top-app-bar__section mdc-top-app-bar__section--align-end"),
          role("toolbar"),
          a(
            className("material-icons mdc-top-app-bar__action-item mdc-icon-button"),
            href("https://github.com/FabioPinheiro/fmgp-generative-design"),
            i(aria.label("Github")),
          ),
          // button(
          //   className("material-icons mdc-top-app-bar__action-item mdc-icon-button"),
          //   aria.label("Search"),
          //   "search"
          // ),
          div(
            className("mdc-menu-surface--anchor"),
            optionsButton,
            options
          )
        ),
      ),
    )
  }

  val drawerScrim = div(className("mdc-drawer-scrim"))
  def drawer(linkPages: List[Page], currentPage: Signal[Page]) =
    aside(
      className("mdc-drawer mdc-drawer--modal"),
      div(
        className("mdc-drawer__header"),
        h3(className("mdc-drawer__title"), "FMGP IPFS"),
        h6(className("mdc-drawer__subtitle"), "fabiomgpinheiro@gmail.com"),
      ),
      div(
        className("mdc-drawer__content"),
        nav(
          className("mdc-list"),
          linkPages.map(page =>
            a(
              className <-- currentPage.map { p =>
                if (p == page) "mdc-list-item mdc-list-item--activated" else "mdc-list-item"
              },
              aria.customProp("current", StringAsIsCodec)("page"),
              tabIndex(0),
              span(className("mdc-list-item__ripple")),
              i(
                className("material-icons mdc-list-item__graphic"),
                aria.hidden(true),
                page.icon
              ),
              navigateTo(page),
              span(className("mdc-list-item__text"), page.title),
            ),
          )
        )
      )
    )

}
