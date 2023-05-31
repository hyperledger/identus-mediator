package fmgp.did.db

import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.Cursor
import zio.Task
import scala.concurrent.ExecutionContext
import zio.ZIO
import reactivemongo.api.CursorProducer

abstract class ReactiveMongoRepo(
    reactiveMongoApi: ReactiveMongoApi,
    collectionName: String,
)(using ec: ExecutionContext) {
  def collection: Task[BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))

  def insertOne[T](value: T)(using
      w: BSONDocumentWriter[T]
  ): Task[WriteResult] = {
    for {
      coll <- collection
      result <- ZIO.fromFuture(implicit ec => coll.insert.one(value))
    } yield result
  }

  def find[T](selector: BSONDocument, projection: Option[BSONDocument])(using
      r: BSONDocumentReader[T],
      p: CursorProducer[T]
  ): Task[List[T]] = {
    for {
      coll <- collection
      result <- ZIO.fromFuture(implicit ec =>
        coll.find(selector, projection).cursor[T]().collect[List](-1, Cursor.FailOnError[List[T]]())
      )
    } yield result
  }

}
