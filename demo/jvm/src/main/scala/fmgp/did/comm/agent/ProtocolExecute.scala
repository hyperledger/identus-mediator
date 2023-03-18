package fmgp.did.comm.agent

import zio._
import zio.json._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.Operations._
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.basicmessage2._
import fmgp.did.comm.protocol.trustping2._

//TODO pick a better name // maybe "Protocol" only

trait ProtocolExecuter[-R] {

  def suportedPIURI: Seq[PIURI]

  /** @return can return a Sync Reply Msg */
  def execute[R1 <: R](plaintextMessage: PlaintextMessage): ZIO[R1, DidFail, Option[EncryptedMessage]] =
    program(plaintextMessage) *> ZIO.none

  def program[R1 <: R](plaintextMessage: PlaintextMessage): ZIO[R1, DidFail, Action]
}

object ProtocolExecuter {
  type Services = Resolver & Agent & Operations & MessageDispatcher
}
case class ProtocolExecuterCollection[-R](executers: ProtocolExecuter[R]*) extends ProtocolExecuter[R] {

  override def suportedPIURI: Seq[PIURI] = executers.flatMap(_.suportedPIURI)

  def selectExecutersFor(piuri: PIURI) = executers.find(_.suportedPIURI.contains(piuri))

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, DidFail, Option[EncryptedMessage]] =
    selectExecutersFor(plaintextMessage.`type`) match
      case None     => NullProtocolExecute.execute(plaintextMessage)
      case Some(px) => px.execute(plaintextMessage)

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, DidFail, Action] =
    selectExecutersFor(plaintextMessage.`type`) match
      case None     => NullProtocolExecute.program(plaintextMessage)
      case Some(px) => px.program(plaintextMessage)
}

trait ProtocolExecuterWithServices[-R <: ProtocolExecuter.Services] extends ProtocolExecuter[R] {

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, DidFail, Option[EncryptedMessage]] =
    program(plaintextMessage)
      .tap(v => ZIO.logDebug(v.toString)) // DEBUG
      .flatMap {
        case _: NoReply.type => ZIO.succeed(None)
        case action: AnyReply /*@ AnyReply(reply)*/ =>
          val reply = action.msg
          for {
            msg <- reply.from match
              case Some(value) => authEncrypt(reply)
              case None        => anonEncrypt(reply)
            // TODO forward message
            maybeSyncReplyMsg <- reply.to match // TODO improve
              case None => ZIO.none
              case Some(send2DIDs) =>
                ZIO
                  .foreach(send2DIDs)(to =>
                    for {
                      messageDispatcher <- ZIO.service[MessageDispatcher]
                      resolver <- ZIO.service[Resolver]
                      doc <- resolver.didDocument(to)
                      url =
                        doc.service.toSeq.flatten // TODO .filter(_.`type`.contend(DIDService.TYPE_DIDCommMessaging))
                        match {
                          case head +: next =>
                            head.serviceEndpoint match
                              case s: String                      => s
                              case s: Seq[URI] @unchecked         => s.head
                              case s: Map[String, URI] @unchecked => s.head._2
                        }
                      jobToRun = ZIO.log(s"Send to url: $url") *> messageDispatcher
                        .send(
                          msg,
                          url, // "http://localhost:8080", // FIXME REMOVE (use for local env)
                          None
                          // url match // FIXME REMOVE (use for local env)
                          //   case http if http.startsWith("http://") => Some(url.drop(7).split(':').head.split('/').head)
                          //   case https if https.startsWith("https://") =>
                          //     Some(url.drop(8).split(':').head.split('/').head)
                          //   case _ => None
                        )
                      _ <- action match
                        case Reply(_)          => jobToRun
                        case SyncReplyOnly(_)  => ZIO.unit
                        case AsyncReplyOnly(_) => jobToRun
                    } yield ()
                  ) *> ZIO
                  .succeed(msg)
                  .when(
                    plaintextMessage.return_route.contains(ReturnRoute.all)
                      && {
                        plaintextMessage.from.map(_.asTO) match {
                          case None          => false
                          case Some(replyTo) => send2DIDs.contains(replyTo)
                        }
                      }
                  )
          } yield maybeSyncReplyMsg
      }

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, DidFail, Action]
}

object NullProtocolExecute extends ProtocolExecuter[Any] {

  override def suportedPIURI = Seq()
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) =
    ZIO.fail(MissingProtocol(plaintextMessage.`type`))
}

object BasicMessageExecuter extends ProtocolExecuter[Any] {

  override def suportedPIURI: Seq[PIURI] = Seq(BasicMessage.piuri)
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) = for {
    job <- BasicMessage.fromPlaintextMessage(plaintextMessage) match
      case Left(error) => ZIO.fail(FailToParse(error))
      case Right(bm)   => Console.printLine(bm.toString).mapError(ex => SomeThrowable(ex))
  } yield NoReply
}

class TrustPingExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services] {

  override def suportedPIURI: Seq[PIURI] = Seq(TrustPing.piuri, TrustPingResponse.piuri)

  override def program[R1 <: Agent](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Action] = {
    // the val is from the match to be definitely stable
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri

    plaintextMessage.`type` match
      case `piuriTrustPing` =>
        TrustPing.fromPlaintextMessage(plaintextMessage) match
          case Left(error)                                    => ZIO.fail(FailToParse(error))
          case Right(ping: TrustPingWithOutRequestedResponse) => ZIO.logInfo(ping.toString()) *> ZIO.succeed(NoReply)
          case Right(ping: TrustPingWithRequestedResponse) =>
            for {
              _ <- ZIO.logInfo(ping.toString())
              agent <- ZIO.service[Agent]
              ret = ping.makeRespond
            } yield Reply(ret.toPlaintextMessage)
      case `piuriTrustPingResponse` =>
        for {
          job <- TrustPingResponse.fromPlaintextMessage(plaintextMessage) match
            case Left(error) => ZIO.fail(FailToParse(error))
            case Right(ping) => ZIO.logInfo(ping.toString())
        } yield NoReply
  }

}
