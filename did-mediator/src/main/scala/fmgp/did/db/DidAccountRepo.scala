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

  def collection: IO[StorageCollection, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))
    .mapError(ex => StorageCollection(ex))

  def newDidAccount(did: DIDSubject /*, alias: Seq[DID] = Seq.empty*/ ): IO[StorageError, WriteResult] = {
    val value = DidAccount(
      did = did,
      alias = Seq(did),
      messagesRef = Seq.empty
    )
    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec => coll.insert.one(value))
        .mapError(ex => StorageThrowable(ex))
    } yield result
  }

  def getDidAccount(did: DIDSubject): IO[StorageError, Option[DidAccount]] = {
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
        .mapError(ex => StorageThrowable(ex))
    } yield result.headOption
  }

  def addAlias(owner: DIDSubject, newAlias: DIDSubject): ZIO[Any, StorageError, Either[String, Unit]] = {
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
        .mapError(ex => StorageThrowable(ex)) // TODO may appropriate error
    } yield Right(())

  }

  def removeAlias(owner: DIDSubject, newAlias: DIDSubject): ZIO[Any, StorageError, Either[String, Unit]] = {
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
        .mapError(ex => StorageThrowable(ex)) // TODO may appropriate error
    } yield Right(())
  }

  /** @return
    *   numbre of documents updated in DB
    */
  def addToInboxes(recipients: Set[DIDSubject], msg: EncryptedMessage): ZIO[Any, StorageError, Int] = {
    def selector =
      BSONDocument(
        "alias" -> BSONDocument("$in" -> recipients.map(_.did)),
        "messagesRef" ->
          BSONDocument(
            "$not" ->
              BSONDocument(
                "$elemMatch" ->
                  BSONDocument(
                    "hash" -> msg.hashCode,
                    "recipient" -> BSONDocument("$in" -> recipients.map(_.did))
                  )
              )
          )
      )

    def update: BSONDocument = BSONDocument(
      "$push" -> BSONDocument(
        "messagesRef" -> BSONDocument(
          "$each" ->
            recipients.map(recipient => MessageMetaData(msg.hashCode, recipient))
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
        .mapError(ex => StorageThrowable(ex)) // TODO may appropriate error
    } yield result.nModified
  }

  def makeAsDelivered(didAccount: DIDSubject, hashs: Seq[HASH]): ZIO[Any, StorageError, Int] = {
    def selector = BSONDocument("did" -> didAccount.did, "messagesRef.hash" -> BSONDocument("$in" -> hashs))
    def update: BSONDocument = BSONDocument("$set" -> BSONDocument("messagesRef.$.state" -> true))

    for {
      coll <- collection
      result <- ZIO
        .fromFuture(implicit ec => coll.update.one(selector, update)) // Just one
        .mapError(ex => StorageThrowable(ex)) // TODO may appropriate error
    } yield result.nModified
  }
}
