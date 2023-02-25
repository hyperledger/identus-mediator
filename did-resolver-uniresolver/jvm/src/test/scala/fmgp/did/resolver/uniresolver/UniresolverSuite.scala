package fmgp.did.resolver.uniresolver

import zio._
import munit._
import fmgp.did._
import fmgp.did.comm._

// FIXME run also on JS: fmgp.did.resolver.uniresolver.UniresolverSuite.ex_did_ion scala.scalajs.js.JavaScriptException: ReferenceError: fetch is not defined
class UniresolverSuite extends ZSuite {

  val resolver = Uniresolver.default

  testZ("ex_did_ion") {
    val d = UniresolverExamples.ex_did_ion
    val got = resolver.didDocument(FROMTO(d))

    got
      .map { doc =>
        assertEquals(doc, UniresolverExamples.ex_did_ion_out_expected.didDocument)
      }
      .mapError(ex => fail(s"Must have no errors: $ex"))
  }

}
