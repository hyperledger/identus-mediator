package io.iohk.atala.mediator

import zio._
import zio.json._
import zio.stream._
import zio.http._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.did.framework._
import io.iohk.atala.mediator.db.{UserAccountRepo, MessageItemRepo}
import io.iohk.atala.mediator.protocols.Problems
import fmgp.did.comm.protocol.reportproblem2.ProblemReport

case class AgentExecutorMediator(
    agent: Agent,
    transportManager: Ref[TransportManager],
    protocolHandler: ProtocolExecuter[OperatorImp.Services, MediatorError | StorageError],
    userAccountRepo: UserAccountRepo,
    messageItemRepo: MessageItemRepo,
) extends AgentExecutar {
  val scope = Scope.global // TODO do not use global
  val indentityLayer = ZLayer.succeed(agent)
  val userAccountRepoLayer = ZLayer.succeed(userAccountRepo)
  val messageItemRepoLayer = ZLayer.succeed(messageItemRepo)
  override def subject: DIDSubject = agent.id.asDIDSubject

  override def acceptTransport(
      transport: TransportDIDComm[Any]
  ): URIO[Operations & Resolver, Unit] =
    transport.inbound
      .mapZIO(msg => jobExecuterProtocol(msg, transport))
      .runDrain
      .forkIn(scope)
      .unit // From Fiber.Runtime[fmgp.util.Transport.InErr, Unit] to Unit

  override def receiveMsg(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any]
  ): URIO[Operations & Resolver, Unit] =
    for {
      job <- acceptTransport(transport)
      ret <- jobExecuterProtocol(msg, transport) // Run a single time (for the message already read)
    } yield ()

  def jobExecuterProtocol(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any],
  ): URIO[Operations & Resolver, Unit] =
    this
      .receiveMessage(msg, transport)
      .tapError(ex => ZIO.logError(ex.toString))
      .provideSomeLayer(this.indentityLayer)
      .provideSomeLayer(userAccountRepoLayer ++ messageItemRepoLayer)
      .provideSomeEnvironment((e: ZEnvironment[Resolver & Operations]) => e ++ ZEnvironment(protocolHandler))
      .orDieWith(ex => new RuntimeException(ex.toString))

  def receiveMessage(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any]
  ): ZIO[
    OperatorImp.Services & ProtocolExecuter[OperatorImp.Services, MediatorError | StorageError],
    MediatorError | StorageError,
    Unit
  ] = ZIO.logAnnotate("msg_sha256", msg.sha256) {
    for {
      _ <- ZIO.logDebug(s"Receive message with sha256: '${msg.sha256}'")
      agent <- ZIO.service[Agent]
      recipientsSubject <- msg match
        case eMsg: EncryptedMessage => ZIO.succeed(eMsg.recipientsSubject)
        case sMsg: SignedMessage =>
          ZIO
            .fromEither(sMsg.payloadAsPlaintextMessage)
            .map(_.to.toSet.flatten.map(_.toDIDSubject))
            .mapError(didFail => MediatorDidError(didFail))
      _ <- transportManager.get.flatMap { m =>
        ZIO.foreach(recipientsSubject)(subject => m.publish(subject.asTO, msg))
      }
      _ <-
        if (!recipientsSubject.contains(agent.id.asDIDSubject)) {
          ZIO.logError(s"This agent '${agent.id.asDIDSubject}' is not a recipient") // TODO send a FAIL!!!!!!
        } else {
          for {
            pMsgOrReplay <- AgentExecutorMediator
              .decrypt(msg)
              .tap { pMsg =>
                pMsg.from match
                  case None       => ZIO.unit
                  case Some(from) => transportManager.update { _.link(from.asFROMTO, transport) }
              }
              .map(Right(_))
              .catchAll { didFail =>
                for {
                  _ <- ZIO.logWarning(s"Error Mediator fail to decrypt: $didFail")
                  agent <- ZIO.service[Agent]
                  problemReport = Problems.decryptFail(
                    from = agent.id.asFROM,
                    comment = "Fail to decrypt Message: " + didFail
                  )
                } yield Left(problemReport)
              }
            _ <- processMessage(msg, pMsgOrReplay, transport)
          } yield ()
        }
    } yield ()
  }

  def processMessage(
      originalMsg: SignedMessage | EncryptedMessage,
      pMsgOrProblemReport: Either[ProblemReport, PlaintextMessage],
      transport: TransportDIDComm[Any]
  ): ZIO[
    ProtocolExecuter[OperatorImp.Services, MediatorError | StorageError] & OperatorImp.Services,
    MediatorError | StorageError,
    Unit
  ] =
    for {
      action <- pMsgOrProblemReport match
        case Left(problemReport) => ZIO.succeed(Reply(problemReport.toPlaintextMessage))
        case Right(plaintextMessage) =>
          for {
            messageItemRepo <- ZIO.service[MessageItemRepo]
            maybeProblemReport <- messageItemRepo
              .insert(originalMsg) // store all message
              .map(_ /*WriteResult*/ => None)
              .catchSome {
                case StorageCollection(error) =>
                  // This deals with connection errors to the database.
                  ZIO.logWarning(s"Error StorageCollection: $error") *>
                    ZIO
                      .service[Agent]
                      .map(agent =>
                        Some(
                          Problems.storageError(
                            to = plaintextMessage.from.map(_.asTO).toSet,
                            from = agent.id,
                            pthid = plaintextMessage.id,
                            piuri = plaintextMessage.`type`,
                          )
                        )
                      )
                case StorageThrowable(error) =>
                  ZIO.logWarning(s"Error StorageThrowable: $error") *>
                    ZIO
                      .service[Agent]
                      .map(agent =>
                        Some(
                          Problems.storageError(
                            to = plaintextMessage.from.map(_.asTO).toSet,
                            from = agent.id,
                            pthid = plaintextMessage.id,
                            piuri = plaintextMessage.`type`,
                          )
                        )
                      )
                case DuplicateMessage(error) =>
                  ZIO.logWarning(s"Error DuplicateMessageError: $error") *>
                    ZIO
                      .service[Agent]
                      .map(agent =>
                        Some(
                          Problems.dejavuError(
                            to = plaintextMessage.from.map(_.asTO).toSet,
                            from = agent.id,
                            pthid = plaintextMessage.id,
                            piuri = plaintextMessage.`type`,
                          )
                        )
                      )
              }
            protocolHandler <- ZIO.service[ProtocolExecuter[OperatorImp.Services, MediatorError | StorageError]]
            goodAction <- maybeProblemReport match
              case Some(problemReport) => ZIO.succeed(Reply(problemReport.toPlaintextMessage))
              case None =>
                protocolHandler
                  .program(plaintextMessage)
                  .catchSome { case ProtocolExecutionFailToParse(failToParse) =>
                    for {
                      _ <- ZIO.logWarning(s"Error ProtocolExecutionFailToParse: $failToParse")
                      agent <- ZIO.service[Agent]
                      problemReport = Problems.malformedError(
                        to = plaintextMessage.from.toSet.map(_.asTO),
                        from = agent.id.asFROM,
                        pthid = plaintextMessage.id,
                        piuri = plaintextMessage.`type`,
                        comment = failToParse.error
                      )
                    } yield (Reply(problemReport.toPlaintextMessage))
                  }
                  .tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
          } yield goodAction
      ret <- action match
        case NoReply => ZIO.unit // TODO Maybe infor transport of immediately reply/close
        case reply: AnyReply =>
          import fmgp.did.comm.Operations._
          for {
            message <- {
              reply.msg.to.toSeq.flatten match {
                case Seq() =>
                  reply.msg.from match
                    case Some(from) => sign(reply.msg)
                    case None => ZIO.logError(s"No sender or recipient: ${reply.msg}") *> ZIO.fail(NoSenderOrRecipient)
                case tos => // TODO FIXME is case is not a response
                  reply.msg.from match
                    case Some(from) => authEncrypt(reply.msg)
                    case None       => anonEncrypt(reply.msg)
              }
            }.mapError(didFail => MediatorDidError(didFail))
            _ <- pMsgOrProblemReport match
              case Left(value) => transport.send(message) // REVIEW we are forcing the message to be synchronous
              case Right(plaintextMessage) => {
                plaintextMessage.return_route match
                  case Some(ReturnRoute.none) | None =>
                    for {
                      transportDispatcher: TransportDispatcher <- transportManager.get
                      _ <- reply.msg.to.toSeq.flatten match {
                        case Seq() =>
                          message match
                            case sMsg: SignedMessage =>
                              transport.send(sMsg) // REVIEW we are forcing the message to be synchronous
                            case eMsg: EncryptedMessage =>
                              ZIO.logWarning("This reply message will be sented to nobody: " + reply.msg.toJson)
                        case tos =>
                          ZIO.foreachParDiscard(tos) { to =>
                            transportDispatcher
                              .send(to = to, msg = message, thid = reply.msg.thid, pthid = reply.msg.pthid)
                              .mapError(didFail => MediatorDidError(didFail))
                          }
                      }
                    } yield ()
                  case Some(ReturnRoute.all) | Some(ReturnRoute.thread) => transport.send(message)
              }
          } yield ()
    } yield ()

}

object AgentExecutorMediator {

  def make[S >: Resolver & Operations](
      agent: Agent,
      protocolHandler: ProtocolExecuter[OperatorImp.Services, MediatorError | StorageError],
      userAccountRepo: UserAccountRepo,
      messageItemRepo: MessageItemRepo,
  ): ZIO[TransportFactory, Nothing, AgentExecutar] =
    for {
      transportManager <- TransportManager.make
      mediator = AgentExecutorMediator(agent, transportManager, protocolHandler, userAccountRepo, messageItemRepo)
    } yield mediator

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
              sm.payload.content.fromJson[Message] match
                case Left(error) => ZIO.fail(FailToParse(error))
                case Right(msg2) => decrypt(msg2)
          }
    } yield (plaintextMessage)

}