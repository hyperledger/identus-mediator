package io.iohk.atala.mediator.comm

case class DispatcherError(error: String)
object DispatcherError {
  def apply(throwable: Throwable) = new DispatcherError(throwable.getClass.getName() + ":" + throwable.getMessage)
}
