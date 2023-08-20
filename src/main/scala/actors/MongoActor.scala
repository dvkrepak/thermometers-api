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

  def getCollection(name: String): MongoCollection[Document] = {
    Try(database.getCollection(name)) match {
      case Success(value) => value
      case Failure(ex) => throw ex
    }
  }

  def receive: Receive = {

    case FindThermometersWithPagination(page: Int, pageSize: Int) =>
      val collection = getCollection("thermometers")
      val findFuture = MongoUtils.findCollectionObjectsWithPagination(collection, page, pageSize)

      handleBasicFindQueryResult(sender(), findFuture,
        "Thermometers",
        "Error occurred during FindAllThermometers")
    case CountThermometers =>
      val collection = getCollection("thermometers")
      val countFuture: Future[Long] = MongoUtils.countCollectionObjects(collection)

      handleBasicQuery(sender(), countFuture,
        "Count Thermometers",
        "Error occurred during CountThermometers")

    case FindThermometer(id: String) =>

      val collection = getCollection("thermometers")
      val findFuture = MongoUtils.findCollectionObjectWithId(collection, id)

      handleBasicFindQueryResult(sender(), findFuture,
        "FindOne Document",
       "Error occurred during FindThermometer")


    case CreateThermometer(thermometer: String) =>

      val collection = getCollection("thermometers")
      val insertFuture = MongoUtils.createCollectionObject(collection, thermometer)

      handleBasicQuery(sender(), insertFuture,
        "InsertedOne Document",
        "Error occurred during CreateThermometer")

    case UpdateThermometer(id: String, json: String) =>

      val collection = getCollection("thermometers")
      val updateFuture = MongoUtils.updateCollectionObject(collection, id, json)

      handleBasicQuery(sender(), updateFuture,
        "UpdatedOne Document",
        "Error occurred during UpdateThermometer")

    case DeleteThermometer(id: String) =>

      val collection = getCollection("thermometers")
      val deleteFuture = MongoUtils.deleteCollectionObject(collection, id)

      handleBasicQuery(sender(), deleteFuture,
      "DeletedOne Document",
      "Error occurred during DeleteThermometer")

    case CreateReport(thermometerAction: String) =>

      val collection = getCollection("thermometerActions")
      val insertFuture = MongoUtils.createCollectionObject(collection, thermometerAction)

      insertFuture.onComplete {
        case Success(result) =>
          log.info(s"Inserted Data: ${result.getInsertedId}")
        case Failure(error) =>
          log.error(s"Error occurred during CreateData: ${error.getMessage}")
      }

    case FindReportWithRangeWithId(id: String, createdAtMin: String, createdAtMax: String) =>

      val collection = getCollection("thermometerActions")
      val findFuture = MongoUtils.findWithDateRangeWithThermometerId(collection, id, createdAtMin, createdAtMax)

      handleBasicFindQueryResult(sender(), findFuture,
        "Thermometer Actions with date filters",
        "Error occurred during FindReportWithRangeWithId")

    case FindReportSummarizedWithPagination(page: Int, pageSize: Int) =>
      val senderRef: ActorRef = sender()

      val cacheKey = Uri(s"api/version/service/reports/list/?page=$page&page_size=$pageSize")
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
          val findFuture: Future[Seq[Document]] = MongoUtils.findSummarizedWithPagination(collection, page, pageSize)

          findFuture.onComplete {
            case Success(resultList) =>
              senderRef ! resultList

              lfuCache.put(cacheKey, findFuture)
              log.info(s"Found ${resultList.size} actions summarized")
            case Failure(error) =>
              log.error(s"Error occurred during FindDataSummaries: ${error.getMessage}")
          }
      }
    case CountSummarizedReports =>
      val collection = getCollection("thermometerActions")
      val countFuture: Future[Long] = MongoUtils.countSummarizedReports(collection)

      handleBasicQuery(sender(), countFuture,
        "Count Reports",
        "Error occurred during CountReports")

    case FindMinimumFromReportsWithRange(createdAtMin: String, createdAtMax: String) =>
      findGeneralReportStatistics(sender(), createdAtMin, createdAtMax,
        MongoUtils.findMinimumDataWithRange,
        "minimum", "FindMinimumFromReportsWithRange")

    case FindMaximumFromReportsWithRange(createdAtMin: String, createdAtMax: String) =>
      findGeneralReportStatistics(sender(), createdAtMin, createdAtMax,
        MongoUtils.findMaximumFromReportsWithRange,
        "maximum", "FindMaximumFromReportsWithRange")

    case FindAverageFromReportsWithRange(createdAtMin: String, createdAtMax: String) =>
      findGeneralReportStatistics(sender(), createdAtMin, createdAtMax,
        MongoUtils.findAverageFromReportsWithRange,
        "average", "FindAverageFromReportsWithRange")

    case FindMedianFromReportsWithRange(createdAtMin: String, createdAtMax: String) =>
      findGeneralReportStatistics(sender(), createdAtMin, createdAtMax,
        MongoUtils.findMedianFromReportsWithRange,
      "median", "FindMedianFromReportsWithRange")
    case unexpectedMessage: Any =>
      val errorText = "MongoActor received an unexpected message"
      log.error(s"$errorText: $unexpectedMessage")
      logError(errorText, new Exception(errorText))
  }

  private def logError(errorText: String,
                       error: Throwable): Unit = {
    val collection = getCollection("errors")
    MongoUtils.saveError(collection, errorText, error.getMessage)
  }

  private def handleBasicError(senderRef: ActorRef, errorMsg: String, error: Throwable): Unit = {
    senderRef ! Status.Failure(error)
    logError(errorMsg, error)
    log.error(s"$errorMsg: ${error.getMessage}")
  }

  private def handleBasicQuery(senderRef: ActorRef,
                               futureResult: Future[Any],
                               logMsg: String,
                               errorMsg: String): Unit = {
    futureResult.onComplete {
      case Success(result) =>
        senderRef ! result
        log.info(s"$logMsg: $result")
      case Failure(error) =>
        handleBasicError(senderRef, errorMsg, error)
    }
  }
  private def handleBasicFindQueryResult(senderRef: ActorRef,
                                findFuture: Future[Seq[Document]],
                                logMsg: String,
                                errorMsg: String): Unit = {
    findFuture.onComplete {
      case Success(result) =>
        val resultList = result.toList
        senderRef ! resultList
        log.info(s"Found ${resultList.size} $logMsg")
      case Failure(error) =>
        handleBasicError(senderRef, errorMsg, error)
    }
  }

  private def findGeneralReportStatistics(senderRef: ActorRef,
                                          createdAtMin: String,
                                          createdAtMax: String,
                                          queryFunction: (MongoCollection[Document], String, String) => Future[Seq[Document]],
                                          logMsg: String,
                                          errorMsg: String): Unit = {
    val collection = getCollection("thermometerActions")
    val findFuture = queryFunction(collection, createdAtMin, createdAtMax)

    findFuture.onComplete {
      case Success(result) =>
        val resultList = result.toList
        senderRef ! resultList
        log.info(s"Found ${resultList.size} Thermometer reports with date filters grouped by $logMsg temperature")
      case Failure(error) =>
        handleBasicError(senderRef,
          "Error occurred during " + errorMsg,
          error)
    }
  }
}
