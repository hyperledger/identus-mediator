package io.iohk.atala.mediator

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import zio.json.*

sealed trait MediatorError

case class MediatorException(fail: MediatorError) extends Exception(fail.toString())

final case class MediatorDidError(val error: DidFail) extends MediatorError
object MediatorDidError {
  def apply(error: DidFail) = new MediatorDidError(error)
}

final case class MediatorThrowable(val error: String) extends MediatorError
object MediatorThrowable {
  def apply(throwable: Throwable) = new MediatorThrowable(throwable.getClass.getName() + ":" + throwable.getMessage)
}

// Storage
case class StorageException(fail: StorageError) extends Exception(fail.toString())

sealed trait StorageError { //  extends MediatorError {
  def error: String
}

final case class StorageCollection(val error: String) extends StorageError
object StorageCollection {
  def apply(throwable: Throwable) = new StorageCollection(throwable.getClass.getName() + ":" + throwable.getMessage)
}

final case class StorageThrowable(val error: String) extends StorageError
object StorageThrowable {
  def apply(throwable: Throwable) = new StorageThrowable(throwable.getClass.getName() + ":" + throwable.getMessage)
}

final case class DuplicateMessage(val error: String) extends StorageError
object DuplicateMessage {
  val code = 11000
  def apply(throwable: Throwable) = new DuplicateMessage(throwable.getClass.getName() + ":" + throwable.getMessage)
}

// ProtocolError
sealed trait ProtocolError extends MediatorError {
  def piuri: PIURI
}

// Protocol

object ProtocolError {
  given decoder: JsonDecoder[ProtocolError] = DeriveJsonDecoder.gen[ProtocolError]
  given encoder: JsonEncoder[ProtocolError] = DeriveJsonEncoder.gen[ProtocolError]
}

case class MissingProtocolError(piuri: PIURI) extends ProtocolError
// case class FailToEncodeMessage(piuri: PIURI, error: String) extends ProtocolError
