package fmgp.did.db

import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.Cursor
import scala.concurrent.ExecutionContext
import zio._
import reactivemongo.api.CursorProducer
import fmgp.crypto.error._
object MessageItemRepo {
  def layer: ZLayer[ReactiveMongoApi, Throwable, MessageItemRepo] =
    ZLayer {
      for {
        ref <- ZIO.service[ReactiveMongoApi]
      } yield MessageItemRepo(ref)(using scala.concurrent.ExecutionContext.global)
    }
}

class MessageItemRepo(reactiveMongoApi: ReactiveMongoApi)(using ec: ExecutionContext) {
  def collectionName: String = "message"

  def collection: IO[DidFail, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))
    .catchAll(ex => ZIO.fail(SomeThrowable(ex)))

  // TODO Rename method
  def insertOne(value: MessageItem): IO[DidFail, WriteResult] = {
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec => coll.insert.one(value))
        .catchAll(ex => ZIO.fail(SomeThrowable(ex)))
    } yield result
  }

  def findById(id: HASH): IO[DidFail, Seq[MessageItem]] = {
    def selector: BSONDocument = BSONDocument("_id" -> id)
    def projection: Option[BSONDocument] = None
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec =>
          coll
            .find(selector, projection)
            .cursor[MessageItem]()
            .collect[Seq](-1, Cursor.FailOnError[Seq[MessageItem]]())
        )
        .catchAll(ex => ZIO.fail(SomeThrowable(ex)))
    } yield result
  }

}
