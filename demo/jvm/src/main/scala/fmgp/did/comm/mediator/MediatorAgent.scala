package fmgp.did.comm.mediator

import zio._
import zio.json._
import zio.http._
import zio.http.model._
import zio.http.socket._

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did.comm._
import fmgp.did.comm.agent._
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.did.resolver.peer.DIDPeer.AgentDIDPeer
import fmgp.did.demo.AgentByHost
import zio.http.socket.SocketApp

case class MediatorAgent(
    id: DIDSubject,
    keyStore: KeyStore, // Shound we make it lazy with ZIO
    didSocketManager: Ref[DIDSocketManager],
    messageDB: Ref[MessageDB],
) {
  private def didSubjectAux = id
  private def keyStoreAux = keyStore.keys.toSeq

  def indentity = new Agent {
    override def id: DID = didSubjectAux
    override def keys: Seq[PrivateKey] = keyStoreAux
  }
  def resolver: Resolver = DynamicResolver(DidPeerResolver, didSocketManager)
  val messageDispatcherLayer: ZLayer[Client, DidFail, MessageDispatcher] =
    MessageDispatcher.layer.mapError(ex => SomeThrowable(ex))

  def protocolExecuter = ProtocolExecuter.getExecuteFor _

  // TODO move to another place & move validations and build a contex
  def decrypt(msg: Message): ZIO[Agent & Resolver & Operations, DidFail, PlaintextMessage] =
    for {
      ops <- ZIO.service[Operations]
      plaintextMessage <- msg match
        case pm: PlaintextMessage => ZIO.succeed(pm)
        case em: EncryptedMessage =>
          {
            em.`protected`.obj match
              case AnonProtectedHeader(epk, apv, typ, enc, alg)            => ops.anonDecrypt(em)
              case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) => ops.authDecrypt(em)
          }.flatMap(decrypt _)
        case sm: SignedMessage =>
          ops.verify(sm).flatMap {
            case false => ZIO.fail(ValidationFailed)
            case true =>
              sm.payload.fromJson[Message] match
                case Left(error) => ZIO.fail(FailToParse(error))
                case Right(msg2) => decrypt(msg2)
          }
    } yield (plaintextMessage)

  def receiveMessage(
      data: String,
      mSocketID: Option[SocketID]
  ): ZIO[Operations & MessageDispatcher, DidFail, Unit] =
    for {
      msg <- data.fromJson[EncryptedMessage] match
        case Left(error) =>
          ZIO.logError(s"Data is not a EncryptedMessage: $error")
            *> ZIO.fail(FailToParse(error))
        case Right(message) =>
          ZIO.logDebug(
            "Message's recipients KIDs: " + message.recipientsKid.mkString(",") +
              "; DID: " + "Message's recipients DIDs: " + message.recipientsSubject.mkString(",")
          ) *> ZIO.succeed(message)
      ret <- receiveMessage(msg, mSocketID)
    } yield (ret)

  def receiveMessage(
      msg: EncryptedMessage,
      mSocketID: Option[SocketID]
  ): ZIO[Operations & MessageDispatcher, DidFail, Unit] =
    ZIO
      .logAnnotate("msgHash", msg.hashCode.toString) {
        for {
          _ <- ZIO.log(s"receiveMessage ${msg.hashCode()}")
          _ <-
            if (!msg.recipientsSubject.contains(id))
              ZIO.logError(s"This mediator '${id.string}' is not a recipient")
            else
              for {
                _ <- messageDB.update(db => db.add(msg))
                plaintextMessage <- decrypt(msg)
                _ <- mSocketID match
                  case None => ZIO.unit
                  case Some(socketID) =>
                    plaintextMessage.from match
                      case None      => ZIO.unit
                      case Some(did) => didSocketManager.update { _.link(did, socketID) }

                // TODO Store context of the decrypt unwarping
                // TODO Store context with MsgID and PIURI
                executer = protocolExecuter(plaintextMessage.`type`)
                job <- executer.execute(plaintextMessage)
              } yield ()
        } yield ()
      }
      .provideSomeEnvironment(env => env.add(indentity).add(resolver))

  def createSocketApp: ZIO[MediatorAgent & Operations & MessageDispatcher, Nothing, zio.http.Response] = {
    val SOCKET_ID = "SocketID"
    val appAux = SocketApp {
      case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
        ZIO.logAnnotate(SOCKET_ID, ch.id) {
          DIDSocketManager.registerSocket(ch)
        }
      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
        ZIO.logAnnotate(SOCKET_ID, ch.id) {
          DIDSocketManager
            .newMessage(ch, text)
            .flatMap { case (socketID, encryptedMessage) => receiveMessage(encryptedMessage, Some(socketID)) }
            .mapError(ex => DidException(ex))
        }
      case ChannelEvent(ch, ChannelEvent.ChannelUnregistered) =>
        ZIO.logAnnotate(SOCKET_ID, ch.id) {
          DIDSocketManager.unregisterSocket(ch)
        }
      case channelEvent =>
        ZIO.logAnnotate(SOCKET_ID, channelEvent.channel.id) {
          ZIO.logError(s"Unknown event type: ${channelEvent.event}")
        }
    }
    appAux.toResponse.provideSomeEnvironment { (env) => env.add(env.get[MediatorAgent].didSocketManager) }
  }
}

object MediatorAgent {

  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, MediatorAgent] = for {
    sm <- DIDSocketManager.make
    db <- Ref.make(MessageDB())
  } yield MediatorAgent(id, keyStore, sm, db)

  def make(agent: AgentDIDPeer): ZIO[Any, Nothing, MediatorAgent] = for {
    sm <- DIDSocketManager.make
    db <- Ref.make(MessageDB())
  } yield MediatorAgent(agent.id, agent.keyStore, sm, db)

  def didCommApp = {
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! if req.headersAsList.exists { h =>
            h.key == "content-type" &&
            (h.value == MediaTypes.SIGNED || h.value == MediaTypes.ENCRYPTED.typ)
          } =>
        for {
          agent <- AgentByHost.getAgentFor(req)
          ret <- agent.createSocketApp
            .provideSomeEnvironment((env: ZEnvironment[Operations & MessageDispatcher]) => env.add(agent))
        } yield (ret)
      case req @ Method.POST -> !! if req.headersAsList.exists { h =>
            h.key == "content-type" &&
            (h.value == MediaTypes.SIGNED || h.value == MediaTypes.ENCRYPTED.typ)
          } =>
        for {
          agent <- AgentByHost.getAgentFor(req)
          data <- req.body.asString
          ret <- agent
            .receiveMessage(data, None)
            .provideSomeEnvironment((env: ZEnvironment[Operations & MessageDispatcher]) => env.add(agent))
            .mapError(fail => DidException(fail))
        } yield Response.ok // TODO [return_route extension](https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/return_route/main.md)
      case Method.POST -> !! =>
        ZIO.succeed(
          Response.text(s"The content-type must be ${MediaTypes.SIGNED.typ} and ${MediaTypes.ENCRYPTED.typ}")
        )
    }: Http[Hub[String] & AgentByHost & Operations & MessageDispatcher, Throwable, Request, Response]
  }
}
