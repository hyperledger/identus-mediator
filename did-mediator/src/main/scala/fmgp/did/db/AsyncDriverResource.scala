package fmgp.did.db

import zio.{TaskLayer, ZIO, ZLayer}
import scala.concurrent.duration.DurationInt
import reactivemongo.api.{AsyncDriver, DB, MongoConnection}
import zio.{Task, ZIO, ZLayer}
import scala.concurrent.ExecutionContext

object AsyncDriverResource {
  private def acquire = ZIO.attempt(AsyncDriver())

  private def release(asyncDriver: AsyncDriver) = ZIO
    .fromFuture(implicit ec => asyncDriver.close(10.seconds))
    .orDie
    .unit

  val layer: TaskLayer[AsyncDriver] = ZLayer.scoped(ZIO.acquireRelease(acquire)(release))

}
