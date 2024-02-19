package io.iohk.atala.mediator.protocols

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.routing2.*
import fmgp.did.comm.protocol.reportproblem2.ProblemReport
import io.iohk.atala.mediator.*
import io.iohk.atala.mediator.db.*
import zio.*
import zio.json.*
import fmgp.did.comm.protocol.pickup3.MessageDelivery

object ForwardMessageExecuter
    extends ProtocolExecuter[
      Resolver & Operations & Agent & UserAccountRepo & MessageItemRepo & Ref[MediatorTransportManager],
      MediatorError | StorageError
    ] {

  override def supportedPIURI: Seq[PIURI] = Seq(ForwardMessage.piuri)

  override def program(plaintextMessage: PlaintextMessage) = {
    // the val is from the match to be definitely stable
    val piuriForwardMessage = ForwardMessage.piuri

    (plaintextMessage.`type` match { case `piuriForwardMessage` => plaintextMessage.toForwardMessage }) match
      case Left(error) => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(m: ForwardMessage) =>
        for {
          _ <- ZIO.logInfo("ForwardMessage")
          repoMessageItem <- ZIO.service[MessageItemRepo]
          repoDidAccount <- ZIO.service[UserAccountRepo]
          recipientsSubject = Set(m.next) // m.msg.recipientsSubject
          numbreOfUpdated <- repoDidAccount.addToInboxes(recipientsSubject, m.msg)
          msg <-
            if (numbreOfUpdated > 0) { // Or maybe we can add all the time
              for {
                _ <- repoMessageItem.insert(m.msg)
                _ <- ZIO.logInfo("Add next msg (of the ForwardMessage) to the Message Repo")

                // For Live Mode
                mediatorTransportManager <- ZIO.service[Ref[MediatorTransportManager]].flatMap(_.get)
                agent <- ZIO.service[Agent]
                messageDelivery = MessageDelivery(
                  thid = m.id, // FIXME what should I put here?
                  from = agent.id.asFROM, // Mediator agent
                  to = m.next.asTO, // Destination of the message that is being forward
                  recipient_did = None,
                  attachments = Map(
                    m.msg.sha256 -> m.msg
                  )
                ).toPlaintextMessage
                eMsgDelivery <- Operations
                  .authEncrypt(messageDelivery)
                  .mapError(didFail => MediatorDidError(didFail))
                _ <- for {
                  maybeDidAccount <- repoDidAccount
                    .getDidAccountFromAlias(m.next)
                    .tapErrorCause(errorCause =>
                      ZIO.logErrorCause("Error when retrieving account for live mode forward message", errorCause)
                    )
                    .catchAll(ex => ZIO.none) // ignoring error
                  ret <- maybeDidAccount match {
                    case None => ZIO.unit // nothing to do
                    case Some(didAccount) =>
                      val accountOwner = didAccount.did
                      mediatorTransportManager
                        .sendForLiveMode(accountOwner.asFROMTO, eMsgDelivery)
                        .mapError(didFail => MediatorDidError(didFail))
                  }
                } yield ret
              } yield NoReply
            } else {
              for {
                _ <- ZIO.logWarning("Note: No update on the DidAccount of the recipients")
                agent <- ZIO.service[Agent]
                problem = plaintextMessage.from match {
                  case Some(to) =>
                    Problems.notEnroledError(
                      to = Some(to.asTO),
                      from = agent.id,
                      pthid = plaintextMessage.id,
                      piuri = plaintextMessage.`type`,
                      didNotEnrolled = m.next,
                    )
                  case None =>
                    Problems.notEnroledError(
                      to = None,
                      from = agent.id,
                      pthid = plaintextMessage.id,
                      piuri = plaintextMessage.`type`,
                      didNotEnrolled = m.next,
                    )
                }
              } yield Reply(problem.toPlaintextMessage)
            }
        } yield msg
  }

}
