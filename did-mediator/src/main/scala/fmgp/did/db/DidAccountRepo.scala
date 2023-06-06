package fmgp.did.db

import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.Cursor
import reactivemongo.api.CursorProducer

import zio._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
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

  def newDidAccount(did: DIDSubject /*, alias: Seq[DID] = Seq.empty*/ ): IO[DidFail, WriteResult] = {
    val value = DidAccount(
      did = did,
      alias = Seq(did),
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

  /** @return
    *   numbre of documents updated in DB
    */
  def addToInboxes(recipients: Set[DIDSubject], msg: EncryptedMessage): ZIO[Any, DidFail, Int] = {
    val selector =
      BSONDocument(
        "alias" -> BSONDocument("$in" -> recipients),
        "messagesRef.hash" -> BSONDocument("$nin" -> msg.hashCode()),
        "messagesRef.recipient" -> BSONDocument("$nin" -> recipients)
      )

    def update: BSONDocument = BSONDocument(
      "$push" -> BSONDocument(
        "messagesRef" -> BSONDocument(
          "$each" ->
            recipients.map(recipient => MessageMetaData(msg.hashCode(), recipient))
        )
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
    } yield result.nModified
  }
}
