package fmgp.did.uniresolver

import munit._
import fmgp.did._
import fmgp.did.comm._

import zio.json._
import zio.json.ast.Json

class ResolverOutputSuite extends ZSuite {
  test("parse ex_did_ion_output") {
    UniresolverExamples.ex_did_ion_out.fromJson[DIDResolutionResult] match
      case Left(error) => fail(error)
      case Right(value) =>
        assertEquals(value, UniresolverExamples.ex_did_ion_out_expected)
  }
}
