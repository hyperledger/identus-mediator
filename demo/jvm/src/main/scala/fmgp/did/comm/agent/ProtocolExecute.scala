package fmgp.did.comm.agent

import zio._
import zio.json._

import fmgp.crypto.error._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2._
import fmgp.did.comm.protocol.trustping2._

//TODO pick a better name

trait ProtocolExecuter {
  def execute(
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[Any, DidFail, Unit]

}

object ProtocolExecuter {

  def getExecuteFor(piuri: PIURI): ProtocolExecuter = {
    // the val is from the match to be definitely stable
    val piuriBasicMessage = BasicMessage.piuri
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri
    piuri match
      case `piuriBasicMessage`      => BasicMessageExecuter
      case `piuriTrustPing`         => TrustPingExecuter
      case `piuriTrustPingResponse` => TrustPingExecuter
      case anyPiuri                 => NullProtocolExecute
  }
}

object NullProtocolExecute extends ProtocolExecuter {
  override def execute(plaintextMessage: PlaintextMessage) =
    ZIO.fail(MissingProtocol(plaintextMessage.`type`))
}

object BasicMessageExecuter extends ProtocolExecuter {
  override def execute(plaintextMessage: PlaintextMessage) = for {
    job <- BasicMessage.fromPlaintextMessage(plaintextMessage) match
      case Left(error) => ZIO.fail(FailToParse(error))
      case Right(bm)   => Console.printLine(bm.toString).mapError(ex => SomeThrowable(ex))
  } yield ()
}

object TrustPingExecuter extends ProtocolExecuter {
  Function
  override def execute(plaintextMessage: PlaintextMessage) = {
    // the val is from the match to be definitely stable
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri

    plaintextMessage.`type` match
      case `piuriTrustPing` =>
        for {
          job <- TrustPing.fromPlaintextMessage(plaintextMessage) match
            case Left(error) => ZIO.fail(FailToParse(error))
            case Right(ping) => Console.printLine(ping.toString).mapError(ex => SomeThrowable(ex))
        } yield ()
      case `piuriTrustPingResponse` =>
        for {
          job <- TrustPingResponse.fromPlaintextMessage(plaintextMessage) match
            case Left(error) => ZIO.fail(FailToParse(error))
            case Right(ping) => Console.printLine(ping.toString).mapError(ex => SomeThrowable(ex))
        } yield ()
  }

}
