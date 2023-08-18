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
import io.iohk.atala.mediator.protocols.NullProtocolExecuter
import zio.*
import zio.json.*
import io.iohk.atala.mediator.protocols.MissingProtocolExecuter

object ActionUtils {

  def packResponse(
      plaintextMessage: Option[PlaintextMessage],
      action: Action
  ): ZIO[Operations & Agent & Resolver & MessageDispatcher, MediatorError, Option[EncryptedMessage]] =
    action match {
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
                    jobToRun <- mURL match
                      case None => ZIO.logWarning(s"No url to send message")
                      case Some(url) => {
                        ZIO.log(s"Send to url: $url") *>
                          messageDispatcher
                            .send(
                              msg,
                              url,
                              None
                              // url match // FIXME REMOVE (use for local env)
                              //   case http if http.startsWith("http://") => Some(url.drop(7).split(':').head.split('/').head)
                              //   case https if https.startsWith("https://") =>
                              //     Some(url.drop(8).split(':').head.split('/').head)
                              //   case _ => None
                            )
                            .catchAll { case DispatcherError(error) => ZIO.logWarning(s"Dispatch Error: $error") }
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
                    plaintextMessage.map(_.return_route).contains(ReturnRoute.all)
                    && {
                      plaintextMessage.flatMap(_.from.map(_.asTO)) match {
                        case None          => false
                        case Some(replyTo) => send2DIDs.contains(replyTo)
                      }
                    }
                  } // || action.isInstanceOf[SyncReplyOnly]
                )
        } yield maybeSyncReplyMsg
    }
}
