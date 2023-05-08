package fmgp.did.demo

import zio._
import zio.Console._
import zio.json._
import zio.json.ast.Json
import fmgp.crypto._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.resolver.peer._

@main def DemoMain() = {
  import Agent0Mediators._
  val program = for {
    _ <- Console.printLine(s"Did: ${agent.id.string}")
    _ <- Console.printLine(s"Agreement Key: ${keyAgreement}")
    _ <- Console.printLine(s"Authentication Key: $keyAuthentication")
    didDoc <- DidPeerResolver.didDocument(agent.id)
    _ <- Console.printLine(s"DID Document: ${didDoc.toJson /*Pretty*/}")
    me = agent
    a1 = Agent1Mediators.agent
    a2 = Agent2Mediators.agent
    msg: PlaintextMessage = PlaintextMessageClass(
      id = MsgID("1"),
      `type` = PIURI("type"),
      to = Some(Set(me.id)), // NotRequired[Set[DIDURLSyntax]],
      from = Some(me.id), // NotRequired[DIDURLSyntax],
      thid = None, // NotRequired[String],
      created_time = None, // NotRequired[UTCEpoch],
      expires_time = None, // NotRequired[UTCEpoch],
      body = Some(
        Json.Obj(
          "a" -> Json.Str("1"),
          "b" -> Json.Str("2")
        )
      ), //  : Required[JSON_RFC7159],
      attachments = None
    )
    sign <- Operations.sign(msg)
    _ <- Console.printLine(s"sign msg: ${sign.toJson /*Pretty*/}")
    // anonMsg <- Operations.anonEncrypt(msg)
    // _ <- Console.printLine(s"auth msg: ${anonMsg.toJson}")
    // msg2 <- Operations.anonDecrypt(anonMsg)
    // _ <- Console.printLine(s"auth decrypt msg: ${msg2.toJson}")
    authMsg <- Operations.authEncrypt(msg)
    msg3 <- Operations.authDecrypt(authMsg)
    _ <- Console.printLine(s"auth msg: ${msg3.toJson /*Pretty*/}")
  } yield ()

  Unsafe.unsafe { implicit unsafe => // Run side efect
    Runtime.default.unsafe
      .run(
        program.provide(
          Operations.layerDefault ++
            Agent0Mediators.agentLayer ++
            DidPeerResolver.layer
        )
      )
      .getOrThrowFiberFailure()
  }
}
