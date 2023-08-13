package actors

import akka.actor.{Actor, ActorRef, Status}
import akka.event.{Logging, LoggingAdapter}
import messages.MongoMessages._
import org.mongodb.scala.bson.{BsonObjectId, Document}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.ExecutionContext
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
    val errorMsg =
      if (_id.length != 24) {
        s"Invalid _id length. Expected length: 24, Actual length: ${_id.length}"
      } else if (!_id.forall(_.isLetterOrDigit)) {
        "Invalid symbols in _id field. Use only letters and digits"
      } else {
        null // No error message
      }

    if (errorMsg != null) {
      log.info(s"Request to filter Thermometer via _id failed. $errorMsg")
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
      val senderRef = sender()

      val collection = getCollection("thermometers")
      val documentsFuture = collection.find().toFuture()

      documentsFuture.onComplete {
        case Success(documents) =>
          val documentsList = documents.toList
          senderRef ! documentsList
          log.info(s"Found ${documentsList.size} documents")
        case Failure(error) =>
          senderRef ! Status.Failure(error)
          log.error(s"Error occurred during query: ${error.getMessage}")
      }(ExecutionContext.global)

    case CreateThermometer(thermometer: String) =>
      val senderRef = sender()

      val collection = getCollection("thermometers")
      val doc = Document(thermometer)

      val insertionFuture = collection.insertOne(doc).head()

      insertionFuture.onComplete {
        case Success(result) =>
          senderRef ! result.getInsertedId
          log.info(s"InsertedOne Document: ${result.getInsertedId}")
        case Failure(error) =>
          senderRef ! Status.Failure(error)
          log.error(s"Error occurred during InsertOne: ${error.getMessage}")
      }(ExecutionContext.global)

    case EditThermometer(_id: String, json: String) =>
      val senderRef: ActorRef = sender()

      if (requireCorrectId(_id, senderRef)) {
        val collection = getCollection("thermometers")
        val filter = Filters.eq("_id", BsonObjectId(_id))
        val update = Document("$set" -> Document(json))

        val updateFuture = collection
          .updateOne(filter, update)
          .toFuture()

        updateFuture.onComplete {
          case Success(result) =>
            senderRef ! result
            log.info(s"UpdatedOne Document: $result")
          case Failure(error) =>
            senderRef ! Status.Failure(error)
            log.error(s"Error occurred during UpdateOne: ${error.getMessage}")
        }(ExecutionContext.global)
      }


    case FindThermometer(_id: String) =>
      val senderRef: ActorRef = sender()

      if (requireCorrectId(_id, senderRef)) {
        val collection = getCollection("thermometers")
        val filter = Filters.eq("_id", BsonObjectId(_id))

        val findFuture = collection
          .find(filter)
          .headOption()

        findFuture.onComplete {
          case Success(Some(result)) =>
            senderRef ! Some(result)
            log.info(s"FindOne Document: $result")
          case Success(None) =>
            senderRef ! None
            log.info(s"FindOne did not find any documents for _id ${_id}")
          case Failure(error) =>
            senderRef ! Status.Failure(error)
            log.error(s"Error occurred during query: ${error.getMessage}")
        }(ExecutionContext.global)
      }


    case DeleteThermometer(_id: String) =>
      val senderRef: ActorRef = sender()

      if (requireCorrectId(_id, senderRef)) {
        val collection = getCollection("thermometers")
        val filter = Filters.eq("_id", BsonObjectId(_id))

        val deleteFuture = collection
          .deleteOne(filter)
          .toFuture()

        deleteFuture.onComplete {
          case Success(result) =>
            senderRef ! result.getDeletedCount
            log.info(s"DeletedOne Document: ${result.getDeletedCount}")
          case Failure(error) =>
            senderRef ! Status.Failure(error)
            log.error(s"Error occurred during query: ${error.getMessage}")
        }(ExecutionContext.global)
      }

    case SaveData(thermometerAction: String) =>
      val senderRef: ActorRef = sender()


      val doc = Document(thermometerAction)
      val collection = getCollection("thermometerActions")

      val insertFuture = collection.insertOne(doc).toFuture()

      insertFuture.onComplete {
        case Success(result) =>
          log.info(s"Inserted Data: ${result.getInsertedId}")
        case Failure(error) =>
          log.error(s"Error occurred during SaveData: ${error.getMessage}")
      }(ExecutionContext.global)
  }
}
