package io.iohk.atala.mediator.db

import fmgp.did.*
import io.iohk.atala.mediator.{StorageCollection, StorageError, StorageThrowable}
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, CursorProducer}
import zio.*

import scala.concurrent.ExecutionContext
object MessageItemRepo {
  def layer: ZLayer[ReactiveMongoApi, Throwable, MessageItemRepo] =
    ZLayer {
      for {
        ref <- ZIO.service[ReactiveMongoApi]
      } yield MessageItemRepo(ref)(using scala.concurrent.ExecutionContext.global)
    }
}

class MessageItemRepo(reactiveMongoApi: ReactiveMongoApi)(using ec: ExecutionContext) {
  def collectionName: String = "messages"

  def collection: IO[StorageCollection, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))
    .mapError(ex => StorageCollection(ex))

  def insert(value: MessageItem): IO[StorageError, WriteResult] = {
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec => coll.insert.one(value))
        .mapError(ex => StorageThrowable(ex))
    } yield result
  }

  def findById(id: HASH): IO[StorageError, Option[MessageItem]] = {
    def selector: BSONDocument = BSONDocument("_id" -> id)
    def projection: Option[BSONDocument] = None
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec =>
          coll
            .find(selector, projection)
            .cursor[MessageItem]()
            .collect[Seq](1, Cursor.FailOnError[Seq[MessageItem]]())
        )
        .mapError(ex => StorageThrowable(ex))
    } yield result.headOption
  }

  def findByIds(ids: Seq[HASH]): IO[StorageError, Seq[MessageItem]] = {
    def selector: BSONDocument = {
      println(s""" {"_id": {"$$in" -> $ids}} """)
      BSONDocument("_id" -> BSONDocument("$in" -> ids))
    }
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
        .mapError(ex => StorageThrowable(ex))
    } yield result
  }

}
