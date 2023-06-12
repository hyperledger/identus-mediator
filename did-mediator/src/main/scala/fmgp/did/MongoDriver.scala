package fmgp.did

import scala.concurrent.{ExecutionContext, Future}

import reactivemongo.api.{Cursor, DB, MongoConnection, AsyncDriver}
import reactivemongo.api.bson.{BSONDocumentWriter, BSONDocumentReader, Macros, document}

object MongoDriver {
  // My settings (see available connection options)
  // val connectionString = "mongodb://localhost:27017/mydb?authMode=scram-sha1"
  val connectionString =
    // "mongodb+srv://mediator:@fmgp-db.orfjsdi.mongodb.net/?retryWrites=true&w=majority"
    "mongodb+srv://mediator:w419cDYIQ2lxpDKT@mediatordb.sa0rfqg.mongodb.net"

  import ExecutionContext.Implicits.global // use any appropriate context

  // Connect to the database: Must be done only once per application
  val driver = AsyncDriver()
  val parsedUri = MongoConnection.fromString(connectionString)

  // Database and collections: Get references
  val futureConnection = parsedUri.flatMap(driver.connect(_))
  def db1: Future[DB] = futureConnection.flatMap(_.database("firstdb"))
  def db2: Future[DB] = futureConnection.flatMap(_.database("anotherdb"))
  def personCollection = db1.map(_.collection("person"))

  // Write Documents: insert or update

  implicit def personWriter: BSONDocumentWriter[Person] = Macros.writer[Person]
  // or provide a custom one

  // use personWriter
  def createPerson(person: Person): Future[Unit] =
    personCollection.flatMap(_.insert.one(person).map(_ => {}))

  def updatePerson(person: Person): Future[Int] = {
    val selector = document(
      "firstName" -> person.firstName,
      "lastName" -> person.lastName
    )

    // Update the matching person
    personCollection.flatMap(_.update.one(selector, person).map(_.n))
  }

  implicit def personReader: BSONDocumentReader[Person] = Macros.reader[Person]
  // or provide a custom one

  def findPersonByAge(age: Int): Future[List[Person]] =
    personCollection.flatMap(
      _.find(document("age" -> age))
        .cursor[Person]()
        .collect[List](-1, Cursor.FailOnError[List[Person]]())
    )
    // ... deserializes the document using personReader

  // Custom persistent types
  case class Person(firstName: String, lastName: String, age: Int)
}
