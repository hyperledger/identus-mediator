package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.resolver.peer.DIDPeer._
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.crypto.error._

import fmgp.did.AgentProvider
import com.raquo.airstream.core.Observable
import com.raquo.airstream.core.Sink
object AgentDB {
  val dbVar: Var[Option[MessageDB]] = Var(initial = None)

  // val job =
  //   Signal
  //     .combine(
  //       Global.agentVar,
  //       encryptedMessageVar
  //     )
  //     .map {
  //       case (None, _) =>
  //         decryptMessageVar.update(_ => None)
  //       case (Some(agent), Left(error)) =>
  //         decryptMessageVar.update(_ => Some(Left(FailToParse("Fail to parse Encrypted Message: " + error))))
  //       case (Some(agent), Right(msg)) =>
  //         val program = {
  //           msg.`protected`.obj match
  //             case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
  //               OperationsClientRPC.anonDecrypt(msg)
  //             case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
  //               OperationsClientRPC.authDecrypt(msg)
  //         }.mapBoth(
  //           error => decryptMessageVar.update(_ => Some(Left(error))),
  //           msg => decryptMessageVar.update(_ => Some(Right(msg)))
  //         )
  //         Unsafe.unsafe { implicit unsafe => // Run side efect
  //           Runtime.default.unsafe.fork(
  //             program.provideEnvironment(ZEnvironment(agent, DidPeerResolver))
  //           )
  //         }
  //     }
  //     .observe(App.owner)

  def updateDB =
    val program: IO[DidFail, Option[MessageDB]] = Client.getDB()
    Unsafe.unsafe { implicit unsafe => // Run side efect
      Runtime.default.unsafe.fork(
        program
          .map(db => dbVar.update(_ => db))
          .tapError(ex => ZIO.logError(ex.toString))
      )
    }

  def jobUpdateFromAgentSignal(owner: Owner) =
    Global.agentVar.signal.map(e => updateDB).observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      jobUpdateFromAgentSignal(ctx.owner)
      updateDB
      ()
    },
    code("AgentDB Page"),
    p(
      overflowWrap.:=("anywhere"),
      "Agent: ",
      " ",
      code(child.text <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("NO AGENT IS SELECTED!")))
    ),
    button(
      "RESET",
      onClick --> Observer(_ => updateDB)
    ),
    div(
      child <-- dbVar.signal.map(_.toJsonPretty)
    )
  )

  def apply(): HtmlElement = rootElement
}
