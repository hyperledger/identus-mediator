package org.hyperledger.identus.mediator.db

import reactivemongo.api.{AsyncDriver, DB, MongoConnection}
import zio.{Task, TaskLayer, ZIO, ZLayer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object AsyncDriverResource {
  private def acquire = ZIO.attempt(AsyncDriver())

  private def release(asyncDriver: AsyncDriver) = ZIO
    .fromFuture(implicit ec => asyncDriver.close(10.seconds))
    .orDie
    .unit

  val layer: TaskLayer[AsyncDriver] = ZLayer.scoped(ZIO.acquireRelease(acquire)(release))

}
