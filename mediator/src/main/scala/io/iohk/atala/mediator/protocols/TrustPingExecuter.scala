package io.iohk.atala.mediator.protocols

import fmgp.crypto.error.FailToParse
import fmgp.did.Agent
import fmgp.did.comm.{PIURI, PlaintextMessage}
import fmgp.did.comm.protocol.trustping2.{
  TrustPing,
  TrustPingResponse,
  TrustPingWithOutRequestedResponse,
  TrustPingWithRequestedResponse
}
import io.iohk.atala.mediator.{MediatorDidError, MediatorError}
import io.iohk.atala.mediator.actions.{Action, NoReply, ProtocolExecuter, ProtocolExecuterWithServices, Reply}
import zio.ZIO

class TrustPingExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services, MediatorError] {

  override def supportedPIURI: Seq[PIURI] = Seq(TrustPing.piuri, TrustPingResponse.piuri)

  override def program[R1 <: Agent](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, MediatorError, Action] = {
    // the val is from the match to be definitely stable
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri

    plaintextMessage.`type` match
      case `piuriTrustPing` =>
        TrustPing.fromPlaintextMessage(plaintextMessage) match
          case Left(error)                                    => ZIO.fail(MediatorDidError(FailToParse(error)))
          case Right(ping: TrustPingWithOutRequestedResponse) => ZIO.logInfo(ping.toString()) *> ZIO.succeed(NoReply)
          case Right(ping: TrustPingWithRequestedResponse) =>
            for {
              _ <- ZIO.logInfo(ping.toString())
              agent <- ZIO.service[Agent]
              ret = ping.makeRespond
            } yield Reply(ret.toPlaintextMessage)
      case `piuriTrustPingResponse` =>
        for {
          job <- TrustPingResponse.fromPlaintextMessage(plaintextMessage) match
            case Left(error) => ZIO.fail(MediatorDidError(FailToParse(error)))
            case Right(ping) => ZIO.logInfo(ping.toString())
        } yield NoReply
  }

}
