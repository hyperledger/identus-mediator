package io.iohk.atala.mediator.actions

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.Operations.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.basicmessage2.*
import fmgp.did.comm.protocol.trustping2.*
import io.iohk.atala.mediator.*
import io.iohk.atala.mediator.comm.*
import io.iohk.atala.mediator.db.*
import zio.*
import zio.json.*
//TODO pick a better name // maybe "Protocol" only

trait ProtocolExecuter[-R] {

  def suportedPIURI: Seq[PIURI]

  /** @return can return a Sync Reply Msg */
  def execute[R1 <: R](plaintextMessage: PlaintextMessage): ZIO[R1, MediatorError, Option[EncryptedMessage]] =
    program(plaintextMessage) *> ZIO.none

  def program[R1 <: R](plaintextMessage: PlaintextMessage): ZIO[R1, MediatorError, Action]
}

object ProtocolExecuter {
  type Services = Resolver & Agent & Operations & MessageDispatcher
}
case class ProtocolExecuterCollection[-R](executers: ProtocolExecuter[R]*) extends ProtocolExecuter[R] {

  override def suportedPIURI: Seq[PIURI] = executers.flatMap(_.suportedPIURI)

  def selectExecutersFor(piuri: PIURI) = executers.find(_.suportedPIURI.contains(piuri))

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, MediatorError, Option[EncryptedMessage]] =
    selectExecutersFor(plaintextMessage.`type`) match
      case None     => NullProtocolExecute.execute(plaintextMessage)
      case Some(px) => px.execute(plaintextMessage)

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
  ): ZIO[R1, MediatorError, Action] =
    selectExecutersFor(plaintextMessage.`type`) match
      case None     => NullProtocolExecute.program(plaintextMessage)
      case Some(px) => px.program(plaintextMessage)
}

trait ProtocolExecuterWithServices[-R <: ProtocolExecuter.Services] extends ProtocolExecuter[R] {

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, MediatorError, Option[EncryptedMessage]] =
    program(plaintextMessage)
      .tap(v => ZIO.logDebug(v.toString)) // DEBUG
      .flatMap {
        case _: NoReply.type => ZIO.succeed(None)
        case action: AnyReply =>
          val reply = action.msg
          for {
            msg <- {
              reply.from match
                case Some(value) => authEncrypt(reply)
                case None        => anonEncrypt(reply)
            }.mapError(fail => MediatorDidError(fail))
            // TODO forward message
            maybeSyncReplyMsg <- reply.to.map(_.toSeq) match // TODO improve
              case None        => ZIO.logWarning("Have a reply but the field 'to' is missing") *> ZIO.none
              case Some(Seq()) => ZIO.logWarning("Have a reply but the field 'to' is empty") *> ZIO.none
              case Some(send2DIDs) =>
                ZIO
                  .foreach(send2DIDs)(to =>
                    val job: ZIO[MessageDispatcher & (Resolver & Any), MediatorError, Matchable] = for {
                      messageDispatcher <- ZIO.service[MessageDispatcher]
                      resolver <- ZIO.service[Resolver]
                      doc <- resolver
                        .didDocument(to)
                        .mapError(fail => MediatorDidError(fail))
                      mURL = doc.service.toSeq.flatten
                        .filter(_.`type` match {
                          case str: String      => str == DIDService.TYPE_DIDCommMessaging
                          case seq: Seq[String] => seq.contains(DIDService.TYPE_DIDCommMessaging)
                        }) match {
                        case head +: next => // FIXME discarte the next
                          head.getServiceEndpointAsURIs.headOption // TODO head
                        case Seq() => None // TODO
                      }
                      jobToRun = mURL match
                        case None => ZIO.logWarning(s"No url to send message")
                        case Some(url) => {
                          ZIO.log(s"Send to url: $url") *>
                            messageDispatcher
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
                              .mapError(fail => MediatorDidError(fail))
                        }

                    } yield (jobToRun)
                    action match
                      case Reply(_)          => job
                      case SyncReplyOnly(_)  => ZIO.unit
                      case AsyncReplyOnly(_) => job
                  ) *> ZIO
                  .succeed(msg)
                  .when(
                    {
                      plaintextMessage.return_route.contains(ReturnRoute.all)
                      && {
                        plaintextMessage.from.map(_.asTO) match {
                          case None          => false
                          case Some(replyTo) => send2DIDs.contains(replyTo)
                        }
                      }
                    } || action.isInstanceOf[SyncReplyOnly]
                  )
          } yield maybeSyncReplyMsg
      }

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, MediatorError, Action]
}

object NullProtocolExecute extends ProtocolExecuter[Any] {

  override def suportedPIURI = Seq()
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) =
    ZIO.fail(MissingProtocolError(plaintextMessage.`type`))
}

object BasicMessageExecuter extends ProtocolExecuter[Any] {

  override def suportedPIURI: Seq[PIURI] = Seq(BasicMessage.piuri)
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) = for {
    job <- BasicMessage.fromPlaintextMessage(plaintextMessage) match
      case Left(error) => ZIO.fail(MediatorDidError(FailToParse(error)))
      case Right(bm)   => Console.printLine(bm.toString).mapError(ex => MediatorThrowable(ex)) 
  } yield NoReply
}

class TrustPingExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services] {

  override def suportedPIURI: Seq[PIURI] = Seq(TrustPing.piuri, TrustPingResponse.piuri)

  override def program[R1 <: Agent](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, MediatorError, Action] = {
    // the val is from the match to be definitely stable
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri

    plaintextMessage.`type` match
      case `piuriTrustPing` =>
        TrustPing.fromPlaintextMessage(plaintextMessage) match
          case Left(error)                                    => ZIO.fail(MediatorDidError(FailToParse(error)))
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
            case Left(error) => ZIO.fail(MediatorDidError(FailToParse(error)))
            case Right(ping) => ZIO.logInfo(ping.toString())
        } yield NoReply
  }

}
