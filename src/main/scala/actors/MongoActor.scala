package actors

import akka.actor.{Actor, ActorRef, Status}
import akka.event.{Logging, LoggingAdapter}
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.model.Uri
import messages.MongoMessages._
import org.mongodb.scala.bson.Document
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}
import utils.{CacheSettings, MongoUtils}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class MongoActor(connectionString: String = "mongodb://localhost:27017",
                 databaseName: String = "optimsys-db")
    extends Actor {

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val database: MongoDatabase = mongoClient.getDatabase(databaseName)
  private val log: LoggingAdapter = Logging(context.system, this)
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  val lfuCache: Cache[Uri, Seq[Document]] = CacheSettings.lfuDocumentCache(context.system)

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

    case FindDataSummarized =>
      val senderRef: ActorRef = sender()

      val cacheKey = Uri("api/version/service/data/list")
      val cachedResponse: Option[Future[Seq[Document]]] = lfuCache.get(cacheKey)

      cachedResponse match {
        case Some(cachedFuture) =>
          // Use cached Future[Seq[Document]]

          cachedFuture.onComplete {
            case Success(resultList) =>
              senderRef ! resultList
              log.info("Cache hit: Found cached data")
            case Failure(error) =>
              log.error(s"Cache hit: Cached Future[Seq[Document]] failed: ${error.getMessage}")
          }

        case None =>
          // Cache was not found
          log.info("Cache miss: Calculating and caching data")

          val collection = getCollection("thermometerActions")
          val findFuture: Future[Seq[Document]] = MongoUtils.findSummaries(collection)

          findFuture.onComplete {
            case Success(resultList) =>
              senderRef ! resultList

              lfuCache.put(cacheKey, findFuture)
              log.info(s"Found ${resultList.size} actions summarized")
            case Failure(error) =>
              log.error(s"Error occurred during FindDataSummaries: ${error.getMessage}")
          }
      }
  }
}
