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
      originalMessage: Option[PlaintextMessage],
      action: Action
  ): ZIO[
    Operations & Agent & Resolver & MessageDispatcher & OutboxMessageRepo,
    MediatorError,
    Option[SignedMessage | EncryptedMessage]
  ] =
    action match {
      case _: NoReply.type => ZIO.succeed(None)
      case action: AnyReply =>
        val reply = action.msg
        for {

          outboxRepo <- ZIO.service[OutboxMessageRepo]
          xRequestId <- ZIO.logAnnotations.map(_.get(XRequestId.value))
          // TODO forward message
          maybeSyncReplyMsg: Option[SignedMessage | EncryptedMessage] <- reply.to.map(_.toSeq) match // TODO improve
            case None =>
              ZIO.logWarning("Have a reply but the field 'to' is missing") *>
                sign(reply)
                  .mapError(fail => MediatorDidError(fail))
                  .map(Some(_))
            case Some(Seq()) =>
              ZIO.logWarning("Have a reply but the field 'to' is empty") *>
                sign(reply)
                  .mapError(fail => MediatorDidError(fail))
                  .map(Some(_))
            case Some(send2DIDs) =>
              for {
                msg <- {
                  reply.from match
                    case Some(value) => authEncrypt(reply)
                    case None        => anonEncrypt(reply)
                }.mapError(fail => MediatorDidError(fail))

                replyViaDIDCommMessagingProgramme = ZIO.foreach(send2DIDs) { to =>
                  for {
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
                        for {
                          _ <- ZIO.log(s"Send to url: $url")
                          response <- messageDispatcher
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

                          _ <- outboxRepo
                            .insert(
                              SentMessageItem(
                                msg = msg,
                                plaintext = reply,
                                recipient = Set(to),
                                distination = Some(url),
                                sendMethod = MessageSendMethod.HTTPS_POST,
                                result = response match
                                  case str: String => Some(str)
                                  case _: Unit     => None
                                ,
                                xRequestId = xRequestId
                              )
                            ) // Maybe fork
                            .catchAll { case error => ZIO.logError(s"Store Outbox Error: $error") }
                        } yield ()
                      }
                  } yield ()
                }
                returnTmp <- action match
                  case Reply(_) =>
                    if (
                      originalMessage // this condition is +- the opposite condition as below
                        .map { oMsg => oMsg.return_route.isEmpty || oMsg.return_route.contains(ReturnRoute.none) }
                        .getOrElse(true) // If originalMessage is None
                    ) (replyViaDIDCommMessagingProgramme *> ZIO.none)
                    else ZIO.some(msg)
                  case SyncReplyOnly(_)  => ZIO.some(msg)
                  case AsyncReplyOnly(_) => replyViaDIDCommMessagingProgramme *> ZIO.none
              } yield (returnTmp)
          _ <- maybeSyncReplyMsg match {
            case None => ZIO.unit
            case Some(msg) =>
              ZIO // Store send message INLINE_REPLY
                .succeed(msg)
                .tap(msg =>
                  outboxRepo
                    .insert(
                      SentMessageItem(
                        msg = msg,
                        plaintext = reply,
                        recipient = reply.to.getOrElse(Set.empty),
                        distination = None,
                        sendMethod = MessageSendMethod.INLINE_REPLY,
                        result = None,
                        xRequestId = xRequestId,
                      )
                    )
                    .catchAll { case error => ZIO.logError(s"Store Outbox Error: $error") }
                )
                .when(
                  originalMessage
                    .map { oMsg =>
                      { // Should replies use the same transport channel?
                        oMsg.return_route.contains(ReturnRoute.all) || oMsg.return_route.contains(ReturnRoute.thread)
                      } && {
                        msg match
                          case sMsg: SignedMessage => true // TODO If the Message is only sign shoud we reply back?
                          case eMsg: EncryptedMessage => // Is the reply back to the original sender/caller?
                            val recipients = eMsg.recipientsSubject.toSeq.map(subject => TO(subject.did))
                            oMsg.from.map(_.asTO).exists(recipients.contains)
                      }
                    }
                    .getOrElse(false) // If originalMessage is None
                )
          }

        } yield maybeSyncReplyMsg
    }
}
