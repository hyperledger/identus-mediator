package io.iohk.atala.mediator.app

import fmgp.crypto.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.oobinvitation.OOBInvitation
import fmgp.did.comm.protocol.reportproblem2.ProblemReport
import io.iohk.atala.mediator.*
import io.iohk.atala.mediator.actions.*
import io.iohk.atala.mediator.comm.*
import io.iohk.atala.mediator.db.*
import io.iohk.atala.mediator.protocols.*
import io.iohk.atala.mediator.utils.*
import io.netty.handler.codec.http.HttpHeaderNames
import reactivemongo.api.bson.{*, given}
import zio.*
import zio.http.*
import zio.json.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.io.Source
import zio.http.Header.AccessControlAllowOrigin
import zio.http.Header.AccessControlAllowMethods

case class MediatorAgent(
    override val id: DID,
    override val keyStore: KeyStore, // Should we make it lazy with ZIO
) extends Agent {
  override def keys: Seq[PrivateKey] = keyStore.keys.toSeq

  type Services = Resolver & Agent & Operations & MessageDispatcher & UserAccountRepo & MessageItemRepo &
    OutboxMessageRepo
  val protocolHandlerLayer: URLayer[UserAccountRepo & MessageItemRepo & OutboxMessageRepo, ProtocolExecuter[
    Services,
    MediatorError | StorageError
  ]] =
    ZLayer.succeed(
      ProtocolExecuterCollection[Services, MediatorError | StorageError](
        BasicMessageExecuter,
        new TrustPingExecuter,
        MediatorCoordinationExecuter,
        ForwardMessageExecuter,
        PickupExecuter,
      )(fallback = MissingProtocolExecuter())
    )

  val messageDispatcherLayer: ZLayer[Client, MediatorThrowable, MessageDispatcher] =
    MessageDispatcherJVM.layer.mapError(ex => MediatorThrowable(ex))

  // TODO move to another place & move validations and build a contex
  def decrypt(msg: Message): ZIO[Agent & Resolver & Operations, MediatorError | ProblemReport, PlaintextMessage] = {
    for {
      ops <- ZIO.service[Operations]
      plaintextMessage <- msg match
        case pm: PlaintextMessage => ZIO.succeed(pm)
        case em: EncryptedMessage =>
          {
            em.`protected`.obj match
              case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
                ops
                  .anonDecrypt(em)
                  .mapError(ex => MediatorDidError(ex))
              case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
                ops
                  .authDecrypt(em)
                  .mapError(ex => MediatorDidError(ex))
          }.flatMap(decrypt _)
        case sm: SignedMessage =>
          ops
            .verify(sm)
            .mapError(ex => MediatorDidError(ex))
            .flatMap {
              case false => ZIO.fail(MediatorDidError(ValidationFailed))
              case true =>
                sm.payload.content.fromJson[Message] match
                  case Left(error) => ZIO.fail(MediatorDidError(FailToParse(error)))
                  case Right(msg2) => decrypt(msg2)
            }
    } yield (plaintextMessage)
  }

  def receiveMessage(data: String): ZIO[
    Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & UserAccountRepo & OutboxMessageRepo,
    MediatorError | StorageError,
    Option[SignedMessage | EncryptedMessage]
  ] =
    for {
      msg <- data.fromJson[EncryptedMessage] match
        case Left(error) =>
          ZIO.logError(s"Data is not a EncryptedMessage: $error")
            *> ZIO.fail(MediatorDidError(FailToParse(error)))
        case Right(message) =>
          ZIO.logDebug(
            "Message's recipients KIDs: " + message.recipientsKid.mkString(",") +
              "; DID: " + "Message's recipients DIDs: " + message.recipientsSubject.mkString(",")
          ) *> ZIO.succeed(message)
      maybeSyncReplyMsg <- receiveMessage(msg)
    } yield (maybeSyncReplyMsg)

  private def receiveMessage(msg: EncryptedMessage): ZIO[
    Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & UserAccountRepo & OutboxMessageRepo,
    MediatorError | StorageError,
    Option[SignedMessage | EncryptedMessage]
  ] =
    ZIO
      .logAnnotate("msgHash", msg.sha1) {
        for {
          _ <- ZIO.log("receivedMessage")
          maybeSyncReplyMsg <-
            if (!msg.recipientsSubject.contains(id))
              ZIO.logError(s"This mediator '${id.string}' is not a recipient") *> ZIO.none
            else
              {
                for {
                  messageItemRepo <- ZIO.service[MessageItemRepo]
                  protocolHandler <- ZIO.service[ProtocolExecuter[Services, MediatorError | StorageError]]
                  plaintextMessage <- decrypt(msg)
                  maybeActionStorageError <- messageItemRepo
                    .insert(msg) // store all message
                    .map(_ /*WriteResult*/ => None)
                    .catchSome {
                      case StorageCollection(error) =>
                        // This deals with connection errors to the database.
                        ZIO.logWarning(s"Error StorageCollection: $error") *>
                          ZIO
                            .service[Agent]
                            .map(agent =>
                              Some(
                                Reply(
                                  Problems
                                    .storageError(
                                      to = plaintextMessage.from.map(_.asTO).toSet,
                                      from = agent.id,
                                      pthid = plaintextMessage.id,
                                      piuri = plaintextMessage.`type`,
                                    )
                                    .toPlaintextMessage
                                )
                              )
                            )
                      case StorageThrowable(error) =>
                        ZIO.logWarning(s"Error StorageThrowable: $error") *>
                          ZIO
                            .service[Agent]
                            .map(agent =>
                              Some(
                                Reply(
                                  Problems
                                    .storageError(
                                      to = plaintextMessage.from.map(_.asTO).toSet,
                                      from = agent.id,
                                      pthid = plaintextMessage.id,
                                      piuri = plaintextMessage.`type`,
                                    )
                                    .toPlaintextMessage
                                )
                              )
                            )
                      case DuplicateMessage(error) =>
                        ZIO.logWarning(s"Error DuplicateMessageError: $error") *>
                          ZIO
                            .service[Agent]
                            .map(agent =>
                              Some(
                                Reply(
                                  Problems
                                    .dejavuError(
                                      to = plaintextMessage.from.map(_.asTO).toSet,
                                      from = agent.id,
                                      pthid = plaintextMessage.id,
                                      piuri = plaintextMessage.`type`,
                                    )
                                    .toPlaintextMessage
                                )
                              )
                            )
                    }
                  // TODO Store context of the decrypt unwarping
                  // TODO SreceiveMessagetore context with MsgID and PIURI
                  agent <- ZIO.service[Agent]
                  ret <- {
                    maybeActionStorageError match
                      case Some(reply) => ActionUtils.packResponse(Some(plaintextMessage), reply)
                      case None        => protocolHandler.execute(plaintextMessage)
                  }.tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
                    .catchSome { case MediatorDidError(error) =>
                      ZIO.logError(s"Error MediatorDidError: $error") *>
                        ActionUtils.packResponse(
                          Some(plaintextMessage),
                          Reply(
                            Problems
                              .malformedError(
                                to = plaintextMessage.from.map(_.asTO).toSet,
                                from = agent.id,
                                pthid = plaintextMessage.id,
                                piuri = plaintextMessage.`type`,
                              )
                              .toPlaintextMessage
                          )
                        )
                    }
                } yield ret
              }.catchAll {
                case ex: MediatorError     => ZIO.fail(ex)
                case pr: ProblemReport     => ActionUtils.packResponse(None, Reply(pr.toPlaintextMessage))
                case ex: StorageCollection => ZIO.fail(ex)
                case ex: StorageThrowable  => ZIO.fail(ex)
                case ex: DuplicateMessage  => ZIO.fail(ex)
              }
        } yield maybeSyncReplyMsg
      }
      .provideSomeLayer( /*resolverLayer ++ indentityLayer ++*/ protocolHandlerLayer)

}

object MediatorAgent {

  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, MediatorAgent] = ZIO.succeed(MediatorAgent(id, keyStore))

  def didCommApp = {
    Routes(
      Method.GET / "headers" -> handler { (req: Request) =>
        val data = req.headers.toSeq.map(e => (e.headerName, e.renderedValue))
        ZIO.succeed(Response.text("HEADERS:\n" + data.mkString("\n") + "\nRemoteAddress:" + req.remoteAddress)).debug
      },
      Method.GET / "health" -> handler { (req: Request) => ZIO.succeed(Response.ok) },
      Method.GET / "version" -> handler { (req: Request) => ZIO.succeed(Response.text(MediatorBuildInfo.version)) },
      Method.GET / "did" -> handler { (req: Request) =>
        for {
          agent <- ZIO.service[MediatorAgent]
          ret <- ZIO.succeed(Response.text(agent.id.string))
        } yield (ret)
      },
      Method.GET / "invitation" -> handler { (req: Request) =>
        for {
          agent <- ZIO.service[MediatorAgent]
          annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
          invitation = OOBInvitation(
            from = agent.id,
            goal_code = Some("request-mediate"),
            goal = Some("RequestMediate"),
            accept = Some(Seq("didcomm/v2")),
          )
          _ <- ZIO.log("New mediate invitation MsgID: " + invitation.id.value)
          ret <- ZIO.succeed(Response.json(invitation.toPlaintextMessage.toJson))
        } yield (ret)
      },
      Method.GET / "invitationOOB" -> handler { (req: Request) =>
        for {
          agent <- ZIO.service[MediatorAgent]
          annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
          invitation = OOBInvitation(
            from = agent.id,
            goal_code = Some("request-mediate"),
            goal = Some("RequestMediate"),
            accept = Some(Seq("didcomm/v2")),
          )
          _ <- ZIO.log("New mediate invitation MsgID: " + invitation.id.value)
          ret <- ZIO.succeed(
            Response.text(
              OutOfBandPlaintext.from(invitation.toPlaintextMessage).makeURI("")
            )
          )
        } yield (ret)
      },
      Method.POST / trailing -> handler { (req: Request) =>
        {
          if (
            req.headers
              .get("content-type")
              .exists { h => h == MediaTypes.SIGNED.typ || h == MediaTypes.ENCRYPTED.typ }
          ) {
            for {
              agent <- ZIO.service[MediatorAgent]
              data <- req.body.asString
                .catchAll(ex => ZIO.fail(Response.badRequest("Unable to read the body of the request")))
              ret <- agent
                .receiveMessage(data)
                .map {
                  case None                          => Response.ok
                  case Some(value: SignedMessage)    => Response.json(value.toJson)
                  case Some(value: EncryptedMessage) => Response.json(value.toJson)
                }
                .catchAll {
                  case MediatorDidError(error) =>
                    ZIO.logError(s"Error MediatorDidError: $error") *>
                      ZIO.succeed(Response.status(Status.BadRequest))
                  case MediatorThrowable(error) =>
                    ZIO.logError(s"Error MediatorThrowable: $error") *>
                      ZIO.succeed(Response.status(Status.BadRequest))
                  case StorageCollection(error) =>
                    ZIO.logError(s"Error StorageCollection: $error") *>
                      ZIO.succeed(Response.status(Status.BadRequest))
                  case StorageThrowable(error) =>
                    ZIO.logError(s"Error StorageThrowable: $error") *>
                      ZIO.succeed(Response.status(Status.BadRequest))
                  case DuplicateMessage(error) =>
                    ZIO.logError(s"Error DuplicateKeyError: $error") *>
                      ZIO.succeed(Response.status(Status.BadRequest))
                  case MissingProtocolError(piuri) =>
                    ZIO.logError(s"MissingProtocolError ('$piuri')") *>
                      ZIO.succeed(Response.status(Status.BadRequest)) // TODO
                }
            } yield ret
          } else
            ZIO
              .logError(s"Request Headers: ${req.headers.mkString(",")}")
              .as(
                Response
                  .text(s"The content-type must be ${MediaTypes.SIGNED.typ} or ${MediaTypes.ENCRYPTED.typ}")
                  .copy(status = Status.BadRequest)
              )
        }
      },
      Method.GET / trailing -> handler { (req: Request) =>
        for {
          agent <- ZIO.service[MediatorAgent]
          _ <- ZIO.log("index.html")
          ret <- ZIO.succeed(IndexHtml.html(agent.id))
        } yield ret
      },
      Method.GET / "public" / string("path") -> handler { (path: String, req: Request) =>
        // RoutesMiddleware
        // TODO https://zio.dev/reference/stream/zpipeline/#:~:text=ZPipeline.gzip%20%E2%80%94%20The%20gzip%20pipeline%20compresses%20a%20stream%20of%20bytes%20as%20using%20gzip%20method%3A
        val fullPath = s"public/$path"
        val classLoader = Thread.currentThread().getContextClassLoader()
        val headerContentType = fullPath match
          case s if s.endsWith(".html") => Header.ContentType(MediaType.text.html)
          case s if s.endsWith(".js")   => Header.ContentType(MediaType.text.javascript)
          case s if s.endsWith(".css")  => Header.ContentType(MediaType.text.css)
          case s if s.endsWith(".svg")  => Header.ContentType(MediaType.image.`svg+xml`)
          case s                        => Header.ContentType(MediaType.text.plain)
        Handler.fromResource(fullPath).map(_.addHeader(headerContentType))
      }.flatten
    )
  }.sandbox.toHttpApp

}
