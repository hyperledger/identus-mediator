package fmgp.did.db

import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.Cursor
import reactivemongo.api.CursorProducer

import zio._
import fmgp.did._
import fmgp.crypto.error._
import scala.concurrent.ExecutionContext

object DidAccountRepo {
  def layer: ZLayer[ReactiveMongoApi, Throwable, DidAccountRepo] =
    ZLayer {
      for {
        ref <- ZIO.service[ReactiveMongoApi]
      } yield DidAccountRepo(ref)(using scala.concurrent.ExecutionContext.global)
    }
}

class DidAccountRepo(reactiveMongoApi: ReactiveMongoApi)(using ec: ExecutionContext) {
  def collectionName: String = "user.account"

  def collection: IO[DidFail, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))
    .catchAll(ex => ZIO.fail(SomeThrowable(ex)))

  def newDidAccount(did: DIDSubject, alias: Seq[DID] = Seq.empty): IO[DidFail, WriteResult] = {
    val value = DidAccount(
      did = did,
      alias = alias,
      messagesRef = Seq.empty
    )
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec => coll.insert.one(value))
        .catchAll(ex => ZIO.fail(SomeThrowable(ex)))
    } yield result
  }

  def getDidAccount(did: DIDSubject) = {
    def selector: BSONDocument = BSONDocument("did" -> did)
    def projection: Option[BSONDocument] = None
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec =>
          coll
            .find(selector, projection)
            .cursor[DidAccount]()
            .collect[Seq](1, Cursor.FailOnError[Seq[DidAccount]]()) // Just one
        )
        .catchAll(ex => ZIO.fail(SomeThrowable(ex)))
    } yield result.headOption
  }

  def addAlias(owner: DIDSubject, newAlias: DIDSubject): ZIO[Any, DidFail, Either[String, Unit]] = {
    def selector: BSONDocument = BSONDocument("did" -> owner)
    def update: BSONDocument = BSONDocument(
      "$push" -> BSONDocument(
        "alias" -> newAlias
      )
    )
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec =>
          coll.update
            .one(selector, update) // Just one
        )
        .catchAll(ex => ZIO.fail(SomeThrowable(ex))) // TODO may appropriate error
    } yield Right(())

  }
  // db.keys.find(_ == newAlias) match
  //   case Some(did) => Left(s"${did} is alredy enrolled for mediation ")
  //   case None =>
  //     alias.find(_._1 == newAlias) match
  //       case Some((a, ower)) => Left(s"$newAlias is alredy an alias of $ower")
  //       case None            => Right(this.copy(alias = alias + (newAlias -> ower)))
  def removeAlias(owner: DIDSubject, newAlias: DIDSubject): ZIO[Any, DidFail, Either[String, Unit]] = {
    def selector: BSONDocument = BSONDocument("did" -> owner)
    def update: BSONDocument = BSONDocument(
      "$pull" -> BSONDocument(
        "alias" -> newAlias
      )
    )
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec =>
          coll.update
            .one(selector, update) // Just one
        )
        .catchAll(ex => ZIO.fail(SomeThrowable(ex))) // TODO may appropriate error
    } yield Right(())
  }
  // alias.find(_._1 == newAlias) match
  //   case None                                           => Left(s"$newAlias is not on DB")
  //   case Some((oldAlias, oldOwer)) if (oldOwer != ower) => Left(s"$newAlias is not owed by $ower")
  //   case Some((oldAlias, oldOwer)) => Right(this.copy(alias = alias.view.filterKeys(_ == newAlias).toMap))

}
