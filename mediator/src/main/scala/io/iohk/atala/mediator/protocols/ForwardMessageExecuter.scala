package io.iohk.atala.mediator.protocols

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.routing2.*
import io.iohk.atala.mediator._
import io.iohk.atala.mediator.actions.*
import io.iohk.atala.mediator.db.*
import zio.*
import zio.json.*

object ForwardMessageExecuter
    extends ProtocolExecuterWithServices[ProtocolExecuter.Services & UserAccountRepo & MessageItemRepo] {

  override def suportedPIURI: Seq[PIURI] = Seq(ForwardMessage.piuri)

  override def program[R1 <: UserAccountRepo & MessageItemRepo](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, MediatorError | StorageError, Action] = {
    // the val is from the match to be definitely stable
    val piuriForwardMessage = ForwardMessage.piuri

    (plaintextMessage.`type` match {
      case `piuriForwardMessage` => plaintextMessage.toForwardMessage
    }).map { case m: ForwardMessage =>
      for {
        _ <- ZIO.logInfo("ForwardMessage")
        repoMessageItem <- ZIO.service[MessageItemRepo]
        repoDidAccount <- ZIO.service[UserAccountRepo]
        recipientsSubject = Set(m.next) // m.msg.recipientsSubject
        numbreOfUpdated <- repoDidAccount.addToInboxes(recipientsSubject, m.msg)
        msg <-
          if (numbreOfUpdated > 0) { // Or maybe we can add all the time
            repoMessageItem.insert(MessageItem(m.msg)) *>
              ZIO.logInfo("Add next msg (of the ForwardMessage) to the Message Repo") // TODO change to debug level
          } else
            ZIO.logWarning("Note: No update on the DidAccount of the recipients")
      } yield None
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program *> ZIO.succeed(NoReply)
  }

}
