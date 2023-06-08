package fmgp.did.comm.mediator

import zio._
import zio.json._
import zio.http._
import zio.http.model._
import zio.http.socket._
import zio.http.socket.SocketApp

import io.netty.handler.codec.http.HttpHeaderNames

import fmgp.util._
import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.did.db._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

import reactivemongo.api.bson.{_, given}
import reactivemongo.api.bson.Macros.{_, given}

case class MediatorAgent(
    override val id: DID,
    override val keyStore: KeyStore, // Shound we make it lazy with ZIO
    didSocketManager: Ref[DIDSocketManager],
    messageDB: Ref[MessageDB],
) extends Agent {
  override def keys: Seq[PrivateKey] = keyStore.keys.toSeq

  // val resolverLayer: ULayer[DynamicResolver] =
  //   DynamicResolver.resolverLayer(didSocketManager)

  type Services = Resolver & Agent & Operations & MessageDispatcher & DidAccountRepo & MessageItemRepo
  val protocolHandlerLayer: URLayer[DidAccountRepo & MessageItemRepo, ProtocolExecuter[Services]] =
    ZLayer.succeed(
      ProtocolExecuterCollection[Services](
        BasicMessageExecuter,
        new TrustPingExecuter,
        MediatorCoordinationExecuter,
        ForwardMessageExecuter,
        PickupExecuter,
      )
    )

  // private def _didSubjectAux = id
  // private def _keyStoreAux = keyStore.keys.toSeq
  // val indentityLayer = ZLayer.succeed(new Agent {
  //   override def id: DID = _didSubjectAux
  //   override def keys: Seq[PrivateKey] = _keyStoreAux
  // })

  val messageDispatcherLayer: ZLayer[Client, MediatorThrowable, MessageDispatcher] =
    MessageDispatcherJVM.layer.mapError(ex => MediatorThrowable(ex))

  // TODO move to another place & move validations and build a contex
  def decrypt(msg: Message): ZIO[Agent & Resolver & Operations, MediatorError, PlaintextMessage] =
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

  def receiveMessage(
      data: String,
      mSocketID: Option[SocketID],
  ): ZIO[
    Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & DidAccountRepo,
    MediatorError,
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
    Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & DidAccountRepo,
    MediatorError,
    Option[EncryptedMessage]
  ] =
    ZIO
      .logAnnotate("msgHash", msg.hashCode.toString) {
        for {
          _ <- ZIO.log(s"receiveMessage with hashCode: ${msg.hashCode}")
          maybeSyncReplyMsg <-
            if (!msg.recipientsSubject.contains(id))
              ZIO.logError(s"This mediator '${id.string}' is not a recipient")
                *> ZIO.none
            else
              for {
                messageItemRepo <- ZIO.service[MessageItemRepo]
                _ <- messageItemRepo.insertOne(MessageItem(msg)) // store all message

                _ <- messageDB.update(db => db.add(msg))
                plaintextMessage <- decrypt(msg)
                _ <- didSocketManager.get.flatMap { m => // TODO HACK REMOVE !!!!!!!!!!!!!!!!!!!!!!!!
                  ZIO.foreach(m.tapSockets)(_.socketOutHub.publish(TapMessage(msg, plaintextMessage).toJson))
                }
                _ <- mSocketID match
                  case None => ZIO.unit
                  case Some(socketID) =>
                    plaintextMessage.from match
                      case None       => ZIO.unit
                      case Some(from) => didSocketManager.update { _.link(from.asFROMTO, socketID) }
                // TODO Store context of the decrypt unwarping
                // TODO SreceiveMessagetore context with MsgID and PIURI
                protocolHandler <- ZIO.service[ProtocolExecuter[Services]]
                ret <- protocolHandler
                  .execute(plaintextMessage)
                  .tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
              } yield ret
        } yield maybeSyncReplyMsg
      }
      .provideSomeLayer( /*resolverLayer ++ indentityLayer ++*/ protocolHandlerLayer)

  def createSocketApp(
      annotationMap: Seq[LogAnnotation]
  ): ZIO[
    MediatorAgent & Resolver & Operations & MessageDispatcher & MessageItemRepo & DidAccountRepo,
    Nothing,
    zio.http.Response
  ] = {
    val SOCKET_ID = "SocketID"
    val appAux = SocketApp {
      case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
          DIDSocketManager.registerSocket(ch)
        }
      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
          DIDSocketManager
            .newMessage(ch, text)
            .flatMap { case (socketID, encryptedMessage) => receiveMessage(encryptedMessage, Some(socketID)) }
            .mapError(ex => MediatorException(ex))
        }
      case ChannelEvent(ch, ChannelEvent.ChannelUnregistered) =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
          DIDSocketManager.unregisterSocket(ch)
        }
      case channelEvent =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelEvent.channel.id), annotationMap: _*) {
          ZIO.logError(s"Unknown event type: ${channelEvent.event}")
        }
    }
    appAux.toResponse.provideSomeEnvironment { (env) => env.add(env.get[MediatorAgent].didSocketManager) }
  }

  def websocketListenerApp(
      annotationMap: Seq[LogAnnotation]
  ): ZIO[MediatorAgent & Operations & MessageDispatcher, Nothing, zio.http.Response] = {
    val SOCKET_ID = "SocketID"
    SocketApp {
      case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
          // ch.writeAndFlush(WebSocketFrame.text("Greetings!")) *>
          //   ch.writeAndFlush(WebSocketFrame.text(s"Tap into ${id.did}")) *>
          DIDSocketManager.tapSocket(id, ch)
        }
      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
          ZIO.logWarning(s"Ignored Message from '${ch.id}'")
        }
      case ChannelEvent(ch, ChannelEvent.ChannelUnregistered) =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, ch.id), annotationMap: _*) {
          DIDSocketManager.unregisterSocket(ch)
        }
      case channelEvent =>
        ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelEvent.channel.id), annotationMap: _*) {
          ZIO.logError(s"Unknown event type: ${channelEvent.event}")
        }
    }.toResponse
      .provideSomeEnvironment { (env) => env.add(env.get[MediatorAgent].didSocketManager) }
  }
}

object MediatorAgent {

  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, MediatorAgent] = for {
    sm <- DIDSocketManager.make
    db <- Ref.make(MessageDB())
  } yield MediatorAgent(id, keyStore, sm, db)

  // def make(agent: AgentDIDPeer): ZIO[Any, Nothing, MediatorAgent] = for {
  //   sm <- DIDSocketManager.make
  //   db <- Ref.make(MessageDB())
  // } yield MediatorAgent(agent.id, agent.keyStore, sm, db)

  def didCommApp = {
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! if req.headersAsList.exists { h =>
            h.key.toString.toLowerCase == "content-type" &&
            (h.value.toString.startsWith(MediaTypes.SIGNED.typ) ||
              h.value.toString.startsWith(MediaTypes.ENCRYPTED.typ))
          } =>
        for {
          agent <- ZIO.service[MediatorAgent]
          annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
          ret <- agent.createSocketApp(annotationMap)
        } yield (ret)
      case Method.GET -> !! / "tap" => // TODO only on dev mode
        for {
          agent <- ZIO.service[MediatorAgent]
          annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
          ret <- agent.websocketListenerApp(annotationMap)
        } yield (ret)
      case req @ Method.POST -> !! if req.headersAsList.exists { h =>
            h.key.toString.toLowerCase == "content-type" &&
            (h.value.toString.startsWith(MediaTypes.SIGNED.typ) ||
              h.value.toString.startsWith(MediaTypes.ENCRYPTED.typ))
          } =>
        for {
          agent <- ZIO.service[MediatorAgent]
          data <- req.body.asString
          maybeSyncReplyMsg <- agent
            .receiveMessage(data, None)
            .mapError(fail => MediatorException(fail))
          ret = maybeSyncReplyMsg match
            case None        => Response.ok
            case Some(value) => Response.json(value.toJson)
        } yield ret

      // TODO [return_route extension](https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/return_route/main.md)
      case req @ Method.POST -> !! =>
        ZIO.succeed(
          Response
            .text(s"The content-type must be ${MediaTypes.SIGNED.typ} or ${MediaTypes.ENCRYPTED.typ}")
            .copy(status = Status.BadRequest)
        )
    }: Http[
      Operations & Resolver & MessageDispatcher & MediatorAgent & MessageItemRepo & DidAccountRepo,
      Throwable,
      Request,
      Response
    ]
  } @@
    HttpAppMiddleware.cors(
      zio.http.middleware.Cors.CorsConfig(
        allowedOrigins = _ => true,
        allowedMethods = Some(Set(Method.GET, Method.POST, Method.OPTIONS)),
      )
    )
    @@ HttpAppMiddleware.updateHeaders(headers =>
      Headers(
        headers.map(h =>
          if (h.key == HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN) {
            Header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
          } else h
        )
      )
    )
}
