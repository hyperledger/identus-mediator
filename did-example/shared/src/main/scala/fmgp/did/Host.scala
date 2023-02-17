package fmgp.did

import zio._
import zio.json._
import zio.stream._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._

/** check with {{{ curl 'https://fabio.did.fmgp.app/headers' -v  }}} */
case class Host(host: String)

object Host {
  val root = Host("did.fmgp.app")
  val fabio = Host("fabio.did.fmgp.app")
  val alice = Host("alice.did.fmgp.app")
  val bob = Host("bob.did.fmgp.app")
  val charlie = Host("charlie.did.fmgp.app")
}
