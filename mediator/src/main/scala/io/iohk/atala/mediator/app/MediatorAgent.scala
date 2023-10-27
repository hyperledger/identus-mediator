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
import reactivemongo.api.bson.Macros.{*, given}
import reactivemongo.api.bson.{*, given}
import zio.*
import zio.http.*
import zio.json.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.io.Source
import zio.http.internal.middlewares.Cors.CorsConfig
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
                  ret <- {
                    maybeActionStorageError match
                      case Some(reply) => ActionUtils.packResponse(Some(plaintextMessage), reply)
                      case None        => protocolHandler.execute(plaintextMessage)
                  }.tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
                } yield ret
              }.catchAll {
                case ex: MediatorError         => ZIO.fail(ex)
                case pr: ProblemReport         => ActionUtils.packResponse(None, Reply(pr.toPlaintextMessage))
                case ex: StorageCollection     => ZIO.fail(ex)
                case ex: StorageThrowable      => ZIO.fail(ex)
                case ex: DuplicateMessage => ZIO.fail(ex)
              }
        } yield maybeSyncReplyMsg
      }
      .provideSomeLayer( /*resolverLayer ++ indentityLayer ++*/ protocolHandlerLayer)

}

object MediatorAgent {

  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, MediatorAgent] = ZIO.succeed(MediatorAgent(id, keyStore))

  def didCommApp = {
    Http.collectZIO[Request] {
      case req @ Method.GET -> Root / "headers" =>
        val data = req.headers.toSeq.map(e => (e.headerName, e.renderedValue))
        ZIO.succeed(Response.text("HEADERS:\n" + data.mkString("\n") + "\nRemoteAddress:" + req.remoteAddress)).debug
      case req @ Method.GET -> Root / "health" => ZIO.succeed(Response.ok)
      case Method.GET -> Root / "version"      => ZIO.succeed(Response.text(MediatorBuildInfo.version))
      case Method.GET -> Root / "did" =>
        for {
          agent <- ZIO.service[MediatorAgent]
          ret <- ZIO.succeed(Response.text(agent.id.string))
        } yield (ret)
      case Method.GET -> Root / "invitation" =>
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
      case Method.GET -> Root / "invitationOOB" =>
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
      case req @ Method.POST -> Root
          if req.headers
            // .header(Header.ContentType) // TODO BUG? this does not work
            .get("content-type")
            .exists { h =>
              // TODO after fix BUG
              // h.mediaType.mainType == "application" &&
              // (h.mediaType.subType == "didcomm-signed+json" || h.mediaType.subType == "didcomm-encrypted+json")
              // TODO after update lib
              // h.mediaType.mainType == ZMediaTypes.mainType &&
              // (h.mediaType.subType == MediaTypes.SIGNED.subType || h.mediaType.subType == MediaTypes.ENCRYPTED.subType)
              h == MediaTypes.SIGNED.typ || h == MediaTypes.ENCRYPTED.typ
            } =>
        for {
          agent <- ZIO.service[MediatorAgent]
          data <- req.body.asString
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
      // TODO [return_route extension](https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/return_route/main.md)
      case req @ Method.POST -> Root =>
        ZIO
          .logError(s"Request Headers: ${req.headers.mkString(",")}")
          .as(
            Response
              .text(s"The content-type must be ${MediaTypes.SIGNED.typ} or ${MediaTypes.ENCRYPTED.typ}")
              .copy(status = Status.BadRequest)
          )
      case req @ Method.GET -> Root => { // html.Html.fromDomElement()
        for {
          agent <- ZIO.service[MediatorAgent]
          _ <- ZIO.log("index.html")
          ret <- ZIO.succeed(IndexHtml.html(agent.id))
        } yield ret
      }
    }: Http[
      Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & UserAccountRepo & OutboxMessageRepo,
      Throwable,
      Request,
      Response
    ]
  } ++ Http
    .fromResource(s"public/webapp-fastopt-bundle.js.gz")
    .map(_.setHeaders(Headers(Header.ContentType(MediaType.application.javascript), Header.ContentEncoding.GZip)))
    .when {
      case Method.GET -> Root / "public" / "webapp-fastopt-bundle.js" => true
      case _                                                          => false
    }
    @@ HttpAppMiddleware.cors(
      CorsConfig(
        allowedOrigin = {
          // case origin @ Origin.Value(_, host, _) if host == "dev" => Some(AccessControlAllowOrigin.Specific(origin))
          case _ => Some(AccessControlAllowOrigin.All)
        },
        allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.OPTIONS),
      )
    )
  // @@
  // HttpAppMiddleware.updateHeaders(headers =>
  //   Headers(
  //     headers.map(h =>
  //       if (h.key == HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN) {
  //         Header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
  //       } else h
  //     )
  //   )
  // )
}
