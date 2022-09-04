package fmgp.did.resolver.web

import munit._
import fmgp.did._

import DIDWebExamples._

class DIDWebSuite extends ZSuite {

  test("url from DID Web") {
    val d = DIDWebExamples.ex4_did_to_url
    val did = DIDWeb(DIDSubject(d._1))
    assertEquals(did.url, d._2)
  }

  test("url from DID Web with path") {
    val d = DIDWebExamples.ex5_did_with_path_to_url
    val did = DIDWeb(DIDSubject(d._1))
    assertEquals(did.url, d._2)
  }

  test("url from DID Web with port and path") {
    val d = DIDWebExamples.ex6_did_with_port_path_to_url
    val did = DIDWeb(DIDSubject(d._1))
    assertEquals(did.url, d._2)
  }

}
