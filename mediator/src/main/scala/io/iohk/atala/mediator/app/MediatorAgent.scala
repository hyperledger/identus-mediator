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
    // didSocketManager: Ref[DIDSocketManager], // FIXME SOCKET
) extends Agent {
  override def keys: Seq[PrivateKey] = keyStore.keys.toSeq

  // val resolverLayer: ULayer[DynamicResolver] =
  //   DynamicResolver.resolverLayer(didSocketManager)

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
      )
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

  def receiveMessage(
      data: String,
      mSocketID: Option[SocketID],
  ): ZIO[
    Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & UserAccountRepo & OutboxMessageRepo,
    MediatorError | StorageError,
    Option[EncryptedMessage]
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
      maybeSyncReplyMsg <- receiveMessage(msg, mSocketID)
    } yield (maybeSyncReplyMsg)

  def receiveMessage(
      msg: EncryptedMessage,
      mSocketID: Option[SocketID]
  ): ZIO[
    Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & UserAccountRepo & OutboxMessageRepo,
    MediatorError | StorageError,
    Option[EncryptedMessage]
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
                    .insert(MessageItem(msg)) // store all message
                    .map(_ /*WriteResult*/ => None
                    // TODO messages already on the database -> so this might be a replay attack
                    )
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
                    }
                  // FIXME SOCKET
                  // _ <- didSocketManager.get.flatMap { m => // TODO HACK REMOVE !!!!!!!!!!!!!!!!!!!!!!!!
                  //   ZIO.foreach(m.tapSockets)(_.socketOutHub.publish(TapMessage(msg, plaintextMessage).toJson))
                  // }
                  // _ <- mSocketID match
                  //   case None => ZIO.unit
                  //   case Some(socketID) =>
                  //     plaintextMessage.from match
                  //       case None => ZIO.unit
                  //       case Some(from) =>
                  //         didSocketManager.update {
                  //           _.link(from.asFROMTO, socketID)
                  //         }
                  // TODO Store context of the decrypt unwarping
                  // TODO SreceiveMessagetore context with MsgID and PIURI
                  ret <- {
                    maybeActionStorageError match
                      case Some(reply) => ActionUtils.packResponse(Some(plaintextMessage), reply)
                      case None        => protocolHandler.execute(plaintextMessage)
                  }.tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
                } yield ret
              }.catchAll {
                case ex: MediatorError     => ZIO.fail(ex)
                case pr: ProblemReport     => ActionUtils.packResponse(None, Reply(pr.toPlaintextMessage))
                case ex: StorageCollection => ZIO.fail(ex)
                case ex: StorageThrowable  => ZIO.fail(ex)
              }
        } yield maybeSyncReplyMsg
      }
      .provideSomeLayer( /*resolverLayer ++ indentityLayer ++*/ protocolHandlerLayer)

  // FIXME SOCKET
  // def createSocketApp(
  //     annotationMap: Seq[LogAnnotation]
  // ): ZIO[
  //   MediatorAgent & Resolver & Operations & MessageDispatcher & MessageItemRepo & UserAccountRepo & OutboxMessageRepo,
  //   Nothing,
  //   zio.http.Response
  // ] = {
  //   val SOCKET_ID = "SocketID"
  //   val appAux = SocketApp {
  //     case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
  //         DIDSocketManager.registerSocket(ch)
  //       }
  //     case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
  //         DIDSocketManager
  //           .newMessage(ch, text)
  //           .flatMap { case (socketID, encryptedMessage) => receiveMessage(encryptedMessage, Some(socketID)) }
  //           .mapError {
  //             case ex: MediatorError => MediatorException(ex)
  //             case ex: StorageError  => StorageException(ex)
  //           }
  //       }
  //     case ChannelEvent(ch, ChannelEvent.ChannelUnregistered) =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
  //         DIDSocketManager.unregisterSocket(ch)
  //       }
  //     case channelEvent =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelEvent.channel.id), annotationMap: _*) {
  //         ZIO.logError(s"Unknown event type: ${channelEvent.event}")
  //       }
  //   }
  //   appAux.toResponse.provideSomeEnvironment { (env) => env.add(env.get[MediatorAgent].didSocketManager) }
  // }

  // FIXME SOCKET
  // def websocketListenerApp(
  //     annotationMap: Seq[LogAnnotation]
  // ): ZIO[MediatorAgent & Operations & MessageDispatcher, Nothing, zio.http.Response] = {
  //   val SOCKET_ID = "SocketID"
  //   SocketApp {
  //     case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
  //         // ch.writeAndFlush(WebSocketFrame.text("Greetings!")) *>
  //         //   ch.writeAndFlush(WebSocketFrame.text(s"Tap into ${id.did}")) *>
  //         DIDSocketManager.tapSocket(id, ch)
  //       }
  //     case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
  //         ZIO.logWarning(s"Ignored Message from '${ch.id}'")
  //       }
  //     case ChannelEvent(ch, ChannelEvent.ChannelUnregistered) =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
  //         DIDSocketManager.unregisterSocket(ch)
  //       }
  //     case channelEvent =>
  //       ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelEvent.channel.id), annotationMap: _*) {
  //         ZIO.logError(s"Unknown event type: ${channelEvent.event}")
  //       }
  //   }.toResponse
  //     .provideSomeEnvironment { (env) => env.add(env.get[MediatorAgent].didSocketManager) }
  // }
}

object MediatorAgent {

  // FIXME SOCKET
  // def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, MediatorAgent] = for {
  //   sm <- DIDSocketManager.make
  // } yield MediatorAgent(id, keyStore, sm)
  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, MediatorAgent] = ZIO.succeed(MediatorAgent(id, keyStore))

  def didCommApp = {
    Http.collectZIO[Request] {
      case req @ Method.GET -> Root / "headers" =>
        println(req.headers.size)
        val data = req.headers.toSeq.map(e => (e.headerName, e.renderedValue))
        ZIO.succeed(Response.text("HEADERS:\n" + data.mkString("\n") + "\nRemoteAddress:" + req.remoteAddress)).debug
      case req @ Method.GET -> Root / "health" => ZIO.succeed(Response.ok)

      // FIXME SOCKET
      // case req @ Method.GET -> Root if req.headersAsList.exists { h =>
      //       h.key.toString.toLowerCase == "content-type" &&
      //       (h.value.toString.startsWith(MediaTypes.SIGNED.typ) ||
      //         h.value.toString.startsWith(MediaTypes.ENCRYPTED.typ))
      //     } =>
      //   for {
      //     agent <- ZIO.service[MediatorAgent]
      //     annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
      //     ret <- agent.createSocketApp(annotationMap)
      //   } yield (ret)

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
          if req
            .header(Header.ContentType)
            .exists { h =>
              h.mediaType.mainType == "application" &&
              (h.mediaType.subType == "didcomm-signed+json" || h.mediaType.subType == "didcomm-encrypted+json")
              // FIXME
              // h.mediaType.mainType == ZMediaTypes.mainType &&
              // (h.mediaType.subType == MediaTypes.SIGNED.subType || h.mediaType.subType == MediaTypes.ENCRYPTED.subType)
            } =>
        for {
          agent <- ZIO.service[MediatorAgent]
          data <- req.body.asString
          ret <- agent
            .receiveMessage(data, None)
            .map {
              case None        => Response.ok
              case Some(value) => Response.json(value.toJson)
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
