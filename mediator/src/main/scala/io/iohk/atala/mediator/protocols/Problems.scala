package io.iohk.atala.mediator.protocols

import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.reportproblem2.*

object Problems {
  def unsupportedProtocolType(
      to: Set[TO],
      from: FROM,
      pthid: MsgID,
      piuri: PIURI,
  ) = ProblemReport(
    // id: MsgID = MsgID(),
    to = to,
    from = from, // Can it be Option?
    pthid = pthid,
    ack = None, // Option[Seq[MsgID]],
    code = ProblemCode.ErroFail("msg", "unsupported"),
    comment = None, // Option[String],
    args = None, // Option[Seq[String]],
    escalate_to = None, // Option[String],
  )

  def unsupportedProtocolRole(
      to: TO,
      from: FROM,
      pthid: MsgID,
      piuri: PIURI,
  ) = ProblemReport(
    // id: MsgID = MsgID(),
    to = Set(to),
    from = from, // Can it be Option?
    pthid = pthid,
    ack = None, // Option[Seq[MsgID]],
    code = ProblemCode.ErroFail("msg", "unsupported"),
    comment = None, // Option[String],
    args = None, // Option[Seq[String]],
    escalate_to = None, // Option[String],
  )

  def protocolNotImplemented(
      to: TO,
      from: FROM,
      pthid: MsgID,
      piuri: PIURI,
  ) = ProblemReport(
    // id: MsgID = MsgID(),
    to = Set(to),
    from = from, // Can it be Option?
    pthid = pthid,
    ack = None, // Option[Seq[MsgID]],
    code = ProblemCode.ErroFail("msg", "unsupported"),
    comment = None, // Option[String],
    args = None, // Option[Seq[String]],
    escalate_to = None, // Option[String],
  )

}
