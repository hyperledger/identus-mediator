package fmgp.did.db

import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.Cursor
import scala.concurrent.ExecutionContext
import zio._
import reactivemongo.api.CursorProducer

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

  def collection: Task[BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))

  def insertOne(value: MessageItem): Task[WriteResult] = {
    for {
      coll <- collection
      result <- ZIO.fromFuture(implicit ec => coll.insert.one(value))
    } yield result
  }

  // def find[T](selector: BSONDocument, projection: Option[BSONDocument])(using
  //     r: BSONDocumentReader[T],
  //     p: CursorProducer[T]
  // ): Task[List[T]] = {
  //   for {
  //     coll <- collection
  //     result <- ZIO.fromFuture(implicit ec =>
  //       coll.find(selector, projection).cursor[T]().collect[List](-1, Cursor.FailOnError[List[T]]())
  //     )
  //   } yield result
  // }

}
