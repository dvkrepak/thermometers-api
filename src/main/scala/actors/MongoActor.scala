package actors

import akka.actor.{Actor, ActorRef, Status}
import akka.event.{Logging, LoggingAdapter}
import messages.MongoMessages._
import org.mongodb.scala.bson.{BsonObjectId, Document}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.{InsertOneResult, UpdateResult}
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

class MongoActor(connectionString: String = "mongodb://localhost:27017",
                 databaseName: String = "optimsys-db")
    extends Actor {

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val database: MongoDatabase = mongoClient.getDatabase(databaseName)
  private val log: LoggingAdapter = Logging(context.system, this)


  /**
   * Checks if the provided `_id` is correct:
   * - Has exactly 24 symbols
   * - Contains only digits and letters
   *
   * @param _id `_id` to be validated.
   * @param senderRef Reference to the sender actor
   * @return `true` if the `_id` has a valid length,
   *         `false` otherwise
   */
  private def requireCorrectId(_id: String, senderRef: ActorRef): Boolean = {
    var errorMsg: String = ""
    var sendErrorMsg: Boolean = false
    // Conditions
    if (_id.length != 24) {
      errorMsg = s"Invalid _id length. Expected length: 24, Actual length: ${_id.length}"
      sendErrorMsg = true
      log.info(s"Request to filter Thermometer via _id failed. Expected length: 24, Actual length: ${_id.length}")
    }
    if (!_id.forall(_.isLetterOrDigit)) {
      errorMsg = s"Invalid symbols in _id field. Use only letters and digits"
      sendErrorMsg = true
      log.info(s"Request to filter Thermometer via _id failed. Invalid symbol(-s) in _id field")
    }
    // Final handler
    if (sendErrorMsg) {
      senderRef ! Status.Failure(new IllegalArgumentException(errorMsg))
      false
    } else {
      true
    }
  }

  private def getCollection(name: String): MongoCollection[Document] = {
    Try(database.getCollection(name)) match {
      case Success(value) => value
      case Failure(ex) => throw ex
    }
  }

  def receive: Receive = {
    case FindAllThermometers =>
      val senderRef: ActorRef = sender()
      val collection: MongoCollection[Document] = getCollection("thermometers")
      val documentsBuffer = ListBuffer.empty[Document]

      collection.find().subscribe(
        (document: Document) => {
          log.info(s"Find Document: ${document.toJson}")
          documentsBuffer += document
        },
        (error: Throwable) => log.error(s"Error occurred during query: ${error.getMessage}"),
        () => {
          senderRef ! documentsBuffer.toList
          log.info("Query completed")
        }
      )

    case CreateThermometer(thermometer: String) =>
      val senderRef: ActorRef = sender()
      val doc: Document = Document(thermometer)
      getCollection("thermometers").insertOne(doc).subscribe(
        (result: InsertOneResult) => {
          senderRef ! result
          log.info(s"InsertedOne Document: $result")
        },
        (error: Throwable) => log.error(s"Error occurred during InsertOne: ${error.getMessage}"),
        () => log.info("InsertOne Completed")
      )

    case EditThermometer(_id: String, json: String) =>
      val senderRef: ActorRef = sender()
      if (requireCorrectId(_id, senderRef)) {
        val collection: MongoCollection[Document] = getCollection("thermometers")
        val updatedDocument: Document = Document("$set" -> Document(json))
        collection.updateOne(Filters.eq("_id", BsonObjectId(_id)), updatedDocument)
          .subscribe(
            (result: UpdateResult) => {
              senderRef ! result
              log.info(s"UpdatedOne Document: $result")
            },
            (error: Throwable) => log.error(s"Error occurred during query: ${error.getMessage}"),
            () => log.info("UpdateOne completed")
          )
      }


    case FindThermometer(_id: String) =>
      val senderRef: ActorRef = sender()
      if (requireCorrectId(_id, senderRef)) {
        var found: Boolean = false
        val collection: MongoCollection[Document] = getCollection("thermometers")
        collection.find(Filters.eq("_id", BsonObjectId(_id)))
          .subscribe(
            (result: Document) => {
              senderRef ! Some(result)
              found = true
              log.info(s"FindOne Document: $result")
            },
            (error: Throwable) => log.error(s"Error occurred during query: ${error.getMessage}"),
            () => {
              if (!found) {
                senderRef ! None
              }
              log.info("FindOne completed")
            }
        )
      }
  }
}
