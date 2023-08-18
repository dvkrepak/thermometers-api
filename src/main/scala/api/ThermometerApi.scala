package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import messages.MongoMessages._
import org.mongodb.scala.bson.{BsonObjectId, Document}
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}

import scala.annotation.unused
import scala.concurrent.Future

trait ThermometerApi {

  def createMongoDbActor(): ActorRef

  implicit val requestTimeout: Timeout
  private lazy val mongoActor: ActorRef = createMongoDbActor()

  private def validateId(id: String, name: String): Unit = {
    if (!Validators.requireCorrectId(id)) {
      throw new IllegalArgumentException(
        s"$name must have exactly 24 letters and consist of hex letters and digits only")
    }
  }

  private def validateDates(createdAtMin: String, createdAtMax: String): Unit = {
    val correctDates = Validators.requireCorrectDateFormat(createdAtMin) &&
      Validators.requireCorrectDateFormat(createdAtMax)

    if (!correctDates) {
      throw new IllegalArgumentException(
        "createdAtMin and createdAtMax must be in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSZ'")
    }
  }

  protected def findThermometersWithPagination(page: Int, pageSize: Int): Future[Seq[Document]] = {
    (mongoActor ? FindThermometersWithPagination(page, pageSize)).mapTo[Seq[Document]]
  }

  def countThermometers(): Future[Long] = {
    (mongoActor ? CountThermometers).mapTo[Long]
  }

  protected def createThermometer(jsonString: String): Future[InsertOneResult] = {
    (mongoActor ? CreateThermometer(jsonString)).mapTo[InsertOneResult]
  }

  protected def editThermometer(thermometerId: String, json: String): Future[UpdateResult] = {
    validateId(thermometerId, "thermometerId")

    (mongoActor ? UpdateThermometer(thermometerId, json)).mapTo[UpdateResult]
  }

  protected def findThermometer(_id: String): Future[Seq[Document]] = {
    validateId(_id, "_id")

    (mongoActor ? FindThermometer(_id)).mapTo[Seq[Document]]
  }

  protected def deleteThermometer(_id: String): Future[DeleteResult] = {
    validateId(_id, "_id")

    (mongoActor ? DeleteThermometer(_id)).mapTo[DeleteResult]
  }

  protected def createReport(json: String): Future[BsonObjectId] = {
    (mongoActor ? CreateReport(json)).mapTo[BsonObjectId]
  }

  protected def findReportWithRangeWithId(thermometerId: String,
                                        createdAtMin: String,
                                        createdAtMax: String): Future[Seq[Document]] = {
    validateId(thermometerId, "thermometerId")
    validateDates(createdAtMin, createdAtMax)

    (mongoActor ? FindReportWithRangeWithId(thermometerId, createdAtMin, createdAtMax)).mapTo[Seq[Document]]
  }

  protected def findReportSummarizedWithPagination(page: Int, pageSize: Int): Future[Seq[Document]] = {
    (mongoActor ? FindReportSummarizedWithPagination(page, pageSize)).mapTo[Seq[Document]]
  }

  protected def countSummarizedReports(): Future[Long] = {
    (mongoActor ? CountSummarizedReports).mapTo[Long]
  }

  protected def findMinimumFromReportsWithRange(createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    validateDates(createdAtMin, createdAtMax)

    (mongoActor ? FindMinimumFromReportsWithRange(createdAtMin, createdAtMax)).mapTo[Seq[Document]]
  }

  protected def findMaximumFromReportsWithRange(createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    validateDates(createdAtMin, createdAtMax)

    (mongoActor ? FindMaximumFromReportsWithRange(createdAtMin, createdAtMax)).mapTo[Seq[Document]]
  }

  protected def findAverageFromReportsWithRange(createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    validateDates(createdAtMin, createdAtMax)

    (mongoActor ? FindAverageFromReportsWithRange(createdAtMin, createdAtMax)).mapTo[Seq[Document]]
  }

  protected def findMedianFromReportsWithRange(createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    validateDates(createdAtMin, createdAtMax)

    (mongoActor ? FindMedianFromReportsWithRange(createdAtMin, createdAtMax)).mapTo[Seq[Document]]
  }
}
