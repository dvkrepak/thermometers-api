package actors

import akka.actor.{Actor, ActorRef, Status}
import akka.event.{Logging, LoggingAdapter}

import messages.MongoMessages._
import utils.MongoUtils
import org.mongodb.scala.bson.Document
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

class MongoActor(connectionString: String = "mongodb://localhost:27017",
                 databaseName: String = "optimsys-db")
    extends Actor {

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val database: MongoDatabase = mongoClient.getDatabase(databaseName)
  private val log: LoggingAdapter = Logging(context.system, this)
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

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
      val findFuture = MongoUtils.findCollectionObjects(collection)

      findFuture.onComplete {
        case Success(documents) =>
          val documentsList = documents.toList
          senderRef ! documentsList
          log.info(s"Found ${documentsList.size} documents")
        case Failure(error) =>
          senderRef ! Status.Failure(error)
          log.error(s"Error occurred during FindAllThermometers: ${error.getMessage}")
      }

    case FindThermometer(_id: String) =>
      val senderRef: ActorRef = sender()

      val collection = getCollection("thermometers")
      val findFuture = MongoUtils.findCollectionObjectWithId(collection, _id)

      findFuture.onComplete {
        case Success(Some(result)) =>
          senderRef ! Some(result)
          log.info(s"FindOne Document: $result")
        case Success(None) =>
          senderRef ! None
          log.info(s"FindOne did not find any documents for _id ${_id}")
        case Failure(error) =>
          senderRef ! Status.Failure(error)
          log.error(s"Error occurred during FindThermometer: ${error.getMessage}")
      }

    case CreateThermometer(thermometer: String) =>
      val senderRef = sender()

      val collection = getCollection("thermometers")
      val insertFuture = MongoUtils.createCollectionObject(collection, thermometer)

      insertFuture.onComplete {
        case Success(result) =>
          senderRef ! result.getInsertedId
          log.info(s"InsertedOne Document: ${result.getInsertedId}")
        case Failure(error) =>
          senderRef ! Status.Failure(error)
          log.error(s"Error occurred during CreateThermometer: ${error.getMessage}")
      }

    case UpdateThermometer(_id: String, json: String) =>
      val senderRef: ActorRef = sender()

      val collection = getCollection("thermometers")
      val updateFuture = MongoUtils.updateCollectionObject(collection, _id, json)

      updateFuture.onComplete {
        case Success(result) =>
          senderRef ! result
          log.info(s"UpdatedOne Document: $result")
        case Failure(error) =>
          senderRef ! Status.Failure(error)
          log.error(s"Error occurred during UpdateThermometer: ${error.getMessage}")
      }

    case DeleteThermometer(_id: String) =>
      val senderRef: ActorRef = sender()

      val collection = getCollection("thermometers")
      val deleteFuture = MongoUtils.deleteCollectionObject(collection, _id)

      deleteFuture.onComplete {
        case Success(result) =>
          senderRef ! result.getDeletedCount
          log.info(s"DeletedOne Document: ${result.getDeletedCount}")
        case Failure(error) =>
          senderRef ! Status.Failure(error)
          log.error(s"Error occurred during DeleteThermometer: ${error.getMessage}")
      }

    case CreateData(thermometerAction: String) =>

      val collection = getCollection("thermometerActions")
      val insertFuture = MongoUtils.createCollectionObject(collection, thermometerAction)

      insertFuture.onComplete {
        case Success(result) =>
          log.info(s"Inserted Data: ${result.getInsertedId}")
        case Failure(error) =>
          log.error(s"Error occurred during CreateData: ${error.getMessage}")
      }

    case FindDataWithRangeWithId(_id: String, createdAtMin: String, createdAtMax: String) =>
      val senderRef: ActorRef = sender()

      val collection = getCollection("thermometerActions")
      val findFuture = MongoUtils.findWithDateRangeWithThermometerId(collection, _id, createdAtMin, createdAtMax)

      findFuture.onComplete {
        case Success(result) =>
          val resultList = result.toList
          senderRef ! resultList
          log.info(s"Found ${resultList.size} Thermometer Actions with date filters")
        case Failure(error) =>
          log.error(s"Error occurred during FindDataWithRangeWithId: ${error.getMessage}")
      }

    case FindDataWithId(thermometerId: String) =>
      val senderRef: ActorRef = sender()

      val collection = getCollection("thermometerActions")
      val findFuture = MongoUtils.findWithThermometerId(collection, thermometerId)

      findFuture.onComplete {
        case Success(result) =>
          val resultList = result.toList
          senderRef ! resultList
          log.info(s"Found ${resultList.size} Thermometer Actions without date filters")
        case Failure(error) =>
          log.error(s"Error occurred during FindDataWithId: ${error.getMessage}")
      }
  }
}
