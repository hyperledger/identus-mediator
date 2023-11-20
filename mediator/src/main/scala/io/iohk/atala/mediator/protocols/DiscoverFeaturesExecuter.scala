package io.iohk.atala.mediator.protocols

import zio.ZIO

import fmgp.crypto.error.FailToParse
import fmgp.did.Agent
import fmgp.did.comm.{PIURI, PlaintextMessage}
import fmgp.did.comm.protocol._
import fmgp.did.comm.protocol.discoverfeatures2._
import io.iohk.atala.mediator.{ProtocolExecutionFailToParse, MediatorError}

object DiscoverFeaturesExecuter extends ProtocolExecuter[Agent, MediatorError] {

  override def supportedPIURI: Seq[PIURI] = Seq(FeatureQuery.piuri, FeatureDisclose.piuri)

  override def program(plaintextMessage: PlaintextMessage): ZIO[Agent, MediatorError, Action] = {
    // the val is from the match to be definitely stable
    val piuriFeatureQuery = FeatureQuery.piuri
    val piuriFeatureDisclose = FeatureDisclose.piuri

    plaintextMessage.`type` match
      case `piuriFeatureQuery` =>
        for {
          ret <- plaintextMessage.toFeatureQuery match
            case Left(error) =>
              ZIO.logError(s"Fail in FeatureQuery: $error") *>
                ZIO.fail(ProtocolExecutionFailToParse(FailToParse(error)))
            case Right(featureQuery) =>
              for {
                _ <- ZIO.logInfo(featureQuery.toString())
                agent <- ZIO.service[Agent]
                allProtocols = Seq(
                  FeatureDisclose.Disclose(
                    `feature-type` = "protocol",
                    id = "https://didcomm.org/routing/2.0",
                    roles = Some(Seq("mediator")), // sender
                  ),
                  FeatureDisclose.Disclose(
                    `feature-type` = "protocol",
                    id = "https://didcomm.org/coordinate-mediation/2.0",
                    roles = Some(Seq("responder")), // requester
                  ),
                  FeatureDisclose.Disclose(
                    `feature-type` = "protocol",
                    id = "https://didcomm.org/messagepickup/3.0",
                    roles = Some(Seq("mediator")), // recipient
                  ),
                  FeatureDisclose.Disclose(
                    `feature-type` = "protocol",
                    id = "https://didcomm.org/trust-ping/2.0",
                    roles = Some(Seq("receiver")), // sender
                  ),
                  FeatureDisclose.Disclose(
                    `feature-type` = "protocol",
                    id = "https://didcomm.org/discover-features/2.0",
                    roles = Some(Seq("responder")), // requester
                  ),
                )
                filter = featureQuery.queries
                  .filter(q => q.`feature-type` == "protocol")
                  .map(q => scala.util.matching.Regex(q.`match`))
                discloses = filter.flatMap(reg => allProtocols.filter(e => reg.matches(e.id))).toSet
              } yield FeatureDisclose(
                thid = Some(featureQuery.id),
                to = Set(featureQuery.from.asTO),
                from = agent.id,
                disclosures = discloses.toSeq,
              )
        } yield Reply(ret.toPlaintextMessage)

      case `piuriFeatureDisclose` =>
        for {
          _ <- plaintextMessage.toFeatureDisclose match
            case Left(error)            => ZIO.fail(ProtocolExecutionFailToParse(FailToParse(error)))
            case Right(featureDisclose) => ZIO.logInfo(featureDisclose.toString())
        } yield NoReply
  }

}
