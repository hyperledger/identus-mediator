package fmgp.did.comm.mediator

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.routing2._
import fmgp.did.db._

object ForwardMessageExecuter
    extends ProtocolExecuterWithServices[
      ProtocolExecuter.Services & DidAccountRepo & MessageItemRepo
    ] {

  override def suportedPIURI: Seq[PIURI] = Seq(ForwardMessage.piuri)

  override def program[R1 <: DidAccountRepo & MessageItemRepo](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Action] = {
    // the val is from the match to be definitely stable
    val piuriForwardMessage = ForwardMessage.piuri

    (plaintextMessage.`type` match {
      case `piuriForwardMessage` => plaintextMessage.toForwardMessage
    }).map { case m: ForwardMessage =>
      for {
        _ <- ZIO.logInfo("ForwardMessage")
        repoMessageItem <- ZIO.service[MessageItemRepo]
        repoDidAccount <- ZIO.service[DidAccountRepo]
        recipientsSubject = Set(m.next) // m.msg.recipientsSubject
        numbreOfUpdated <- repoDidAccount.addToInboxes(recipientsSubject, m.msg)
        msg <-
          if (numbreOfUpdated > 0) { // Or maybe we can add all the time
            repoMessageItem.insertOne(MessageItem(m.msg)) *>
              ZIO.logInfo("Add next msg (of the ForwardMessage) to the Message Repo") // TODO change to debug level
          } else
            ZIO.logWarning("Note: No update on the DidAccount of the recipients")
      } yield None
    } match
      case Left(error)    => ZIO.logError(error) *> ZIO.succeed(NoReply)
      case Right(program) => program *> ZIO.succeed(NoReply)
  }

}
