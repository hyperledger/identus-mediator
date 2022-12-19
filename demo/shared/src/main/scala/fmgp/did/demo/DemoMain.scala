package fmgp.did.demo

import zio._
import zio.Console._
import zio.json._
import fmgp.crypto._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.example._
import fmgp.did.resolver.peer._
// object DemoMain extends ZIOAppDefault
@main def DemoMain() = {
  import AgentEX0._
  val program = for {
    _ <- Console.printLine(
      """██████╗ ██╗██████╗     ██████╗ ███████╗███╗   ███╗ ██████╗ 
        |██╔══██╗██║██╔══██╗    ██╔══██╗██╔════╝████╗ ████║██╔═══██╗
        |██║  ██║██║██║  ██║    ██║  ██║█████╗  ██╔████╔██║██║   ██║
        |██║  ██║██║██║  ██║    ██║  ██║██╔══╝  ██║╚██╔╝██║██║   ██║
        |██████╔╝██║██████╔╝    ██████╔╝███████╗██║ ╚═╝ ██║╚██████╔╝
        |╚═════╝ ╚═╝╚═════╝     ╚═════╝ ╚══════╝╚═╝     ╚═╝ ╚═════╝ """.stripMargin
    )
    _ <- Console.printLine(s"Did: $did")
    _ <- Console.printLine(s"Agreement Key: $keyAgreement")
    _ <- Console.printLine(s"Authentication Key: $keyAuthentication")
    didDoc <- DidPeerResolver.didDocument(did)
    _ <- Console.printLine(s"DID Document: ${didDoc.toJson /*Pretty*/}")
    me <- ZIO.service[Agent]
    a1 <- ZIO.service[AgentEX1.type]
    a2 <- ZIO.service[AgentEX2.type]
    msg: PlaintextMessage = PlaintextMessageClass(
      id = "1",
      `type` = "type",
      to = Some(Set(me.id)), // NotRequired[Set[DIDURLSyntax]],
      from = Some(me.id), // NotRequired[DIDURLSyntax],
      thid = None, // NotRequired[String],
      created_time = None, // NotRequired[UTCEpoch],
      expires_time = None, // NotRequired[UTCEpoch],
      body = Map(
        "a" -> "1",
        "b" -> "2"
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

  val operations: ULayer[Operations] = ZLayer.succeed(new MyOperations())
  val resolvers = ZLayer.succeed(DidPeerResolver)

  Unsafe.unsafe { implicit unsafe => // Run side efect
    Runtime.default.unsafe
      .run(
        program.provide(
          operations ++
            AgentEX0.agentLayer ++
            Agents.layerEX1 ++
            Agents.layerEX2 ++
            resolvers
        )
      )
      .getOrThrowFiberFailure()
  }
}
