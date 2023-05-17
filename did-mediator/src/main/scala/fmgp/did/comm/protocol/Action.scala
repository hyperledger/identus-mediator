package fmgp.did.comm.protocol

import fmgp.did.comm.PlaintextMessage

sealed trait Action
object NoReply extends Action
sealed trait AnyReply extends Action { def msg: PlaintextMessage }
// object AnyReply { def unapply(anyReply: AnyReply): Option[PlaintextMessage] = Some(anyReply.msg) }
sealed trait SyncReply extends AnyReply
sealed trait AsyncReply extends AnyReply
case class Reply(msg: PlaintextMessage) extends SyncReply with AsyncReply
case class SyncReplyOnly(msg: PlaintextMessage) extends SyncReply
case class AsyncReplyOnly(msg: PlaintextMessage) extends AsyncReply
