package fmgp.did.comm.mediator

import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto._
import fmgp.did.comm._
import fmgp.did.comm.agent._
import fmgp.crypto.error._
import fmgp.did.resolver.peer.DidPeerResolver
import fmgp.did.resolver.peer.DIDPeer.AgentDIDPeer

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

  def resolver = DynamicResolver(DidPeerResolver, didSocketManager)

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

  def receiveMessage(msg: EncryptedMessage, socketID: SocketID): ZIO[Operations, DidFail, Unit] =
    (for {
      _ <- ZIO.logAnnotateScoped("msgHash", msg.hashCode.toString)
      _ <- ZIO.log(s"receiveMessage ${msg.hashCode()}")
      aaaaa = println("msg.recipients")
      zzz = println(msg.recipients)
      aaa = println(msg.recipientsSubject)
      bbb = println(id)

// demoJVM List(Recipient([B@65d9849f,RecipientHeader(VerificationMethodReferenced(did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y))))
// demoJVM Set(did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0)
// demoJVM     did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo0NDMvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ

      _ <-
        if (!msg.recipientsSubject.contains(id))
          ZIO.logError(s"This mediator '${id.string}' is not a recipient")
        else
          for {
            _ <- messageDB.update(db => db.add(msg))
            plaintextMessage <- decrypt(msg)
            _ <- plaintextMessage.from match
              case None        => ZIO.unit
              case Some(value) => didSocketManager.update { _.link(value, socketID) }

            // TODO Store context of the decrypt unwarping
            // TODO Store context with MsgID and PIURI
            executer = protocolExecuter(plaintextMessage.`type`)
            job <- executer.execute(plaintextMessage)
          } yield ()
    } yield ())
      .provideSomeLayer(Scope.default)
      .provideSomeEnvironment(env => env.add(indentity).add(resolver))

  def createSocketApp: ZIO[MediatorAgent & Operations, Nothing, zio.http.Response] = {
    val constantName_SocketID = "SocketID"
    import zio.http._
    import zio.http.socket._
    val appAux: SocketApp[Ref[DIDSocketManager] & Operations] = SocketApp {
      case ChannelEvent(ch, ChannelEvent.UserEventTriggered(ChannelEvent.UserEvent.HandshakeComplete)) =>
        ZIO.logAnnotate(constantName_SocketID, ch.id) {
          DIDSocketManager.registerSocket(ch)
        }
      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Text(text))) =>
        ZIO.logAnnotate(constantName_SocketID, ch.id) {
          DIDSocketManager
            .newMessage(ch, text)
            .flatMap { case (socketID, encryptedMessage) =>
              receiveMessage(encryptedMessage, socketID)
            }
            .mapError(ex => DidException(ex))
        }
      case ChannelEvent(ch, ChannelEvent.ChannelUnregistered) =>
        ZIO.logAnnotate(constantName_SocketID, ch.id) {
          DIDSocketManager.unregisterSocket(ch)
        }
      case channelEvent =>
        ZIO.logAnnotate(constantName_SocketID, channelEvent.channel.id) {
          ZIO.logError(s"Unknown event type: ${channelEvent.event}")
        }
    }
    appAux.toResponse.provideSomeEnvironment { (env: ZEnvironment[MediatorAgent & Operations]) =>
      env.add(env.get[MediatorAgent].didSocketManager)
    }
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

}