package utils

import org.bson.conversions.Bson
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}

import scala.annotation.unused
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MongoUtils {


  private def findDocumentsWithFilter(collection: MongoCollection[Document],
                                      filter: Bson): Future[Seq[Document]] = {
    collection.find(filter).toFuture()
  }

  private def findDocumentsWithAggregation(collection: MongoCollection[Document],
                                           aggregation: Seq[Bson]): Future[Seq[Document]] = {
    collection.aggregate(aggregation).toFuture()
  }

  private def insertDocument(collection: MongoCollection[Document], obj: Document): Future[InsertOneResult] = {

    collection.insertOne(obj).toFuture()
  }

  private def updateDocument(collection: MongoCollection[Document], filter: Bson,
                             update: Bson): Future[UpdateResult] = {
    collection.updateOne(filter, update).toFuture()
  }


  private def deleteDocument(collection: MongoCollection[Document], filter: Bson): Future[DeleteResult] = {
    collection.deleteOne(filter).toFuture()
  }

  @unused
  def findDocuments(collection: MongoCollection[Document]): Future[Seq[Document]] = {
    collection.find().toFuture()
  }

  def findDocumentsWithPagination(collection: MongoCollection[Document],
                                          page: Int, pageSize: Int): Future[Seq[Document]] = {
    val offset = (page - 1) * pageSize
    collection.find().skip(offset).limit(pageSize).toFuture()
  }

  def countDocuments(collection: MongoCollection[Document]): Future[Long] = {
    collection.countDocuments().toFuture()
  }

  def findDocumentWithId(collection: MongoCollection[Document], id: String): Future[Seq[Document]] = {
    val filter = MongoFilters.idFilter(id)
    findDocumentsWithFilter(collection, filter)
  }

  def deleteDocument(collection: MongoCollection[Document], id: String): Future[DeleteResult] = {
    val filter = MongoFilters.idFilter(id)
    deleteDocument(collection, filter)
  }

  def createDocument(collection: MongoCollection[Document], obj: String): Future[InsertOneResult] = {
    val doc = Document(obj)
    insertDocument(collection, doc)
  }

  def updateDocument(collection: MongoCollection[Document], id: String, data: String): Future[UpdateResult] = {
    val filter = MongoFilters.idFilter(id)
    val update = Document("$set" -> Document(data))
    updateDocument(collection, filter, update)
  }

  def findWithDateRangeWithThermometerId(collection: MongoCollection[Document],
                                         thermometerId: String,
                                         createdAtMin: String,
                                         createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.thermometerIdWithDateRangeFilter(thermometerId, createdAtMin, createdAtMax)
    findDocumentsWithFilter(collection, filter)
  }

  def findSummarizedWithPagination(collection: MongoCollection[Document],
                                   page: Int, pageSize: Int): Future[Seq[Document]] = {
    val aggregation = MongoAggregations.summaryAggregationWithPagination(page, pageSize)
    findDocumentsWithAggregation(collection, aggregation)
  }

  def countSummarizedReports(collection: MongoCollection[Document]): Future[Long] = {
    // Count unique thermometerId values
    val aggregation = MongoAggregations.thermometerIdGroup
    findDocumentsWithAggregation(collection, aggregation).map(_.length.asInstanceOf[Long])
  }

  def findMinimumDataWithRange(collection: MongoCollection[Document],
                               createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.minimumDataWithRange(filter)

    findDocumentsWithAggregation(collection, aggregation)
  }

  def findMaximumFromReportsWithRange(collection: MongoCollection[Document],
                                      createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.maximumDateWithRange(filter)

    findDocumentsWithAggregation(collection, aggregation)
  }

  def findAverageFromReportsWithRange(collection: MongoCollection[Document],
                                      createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.averageDateWithRange(filter)

    findDocumentsWithAggregation(collection, aggregation)
  }

  def findMedianFromReportsWithRange(collection: MongoCollection[Document],
                                     createdAtMin: String, createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.medianDateWithRange(filter)

    findDocumentsWithAggregation(collection, aggregation)
  }

  def saveError(collection: MongoCollection[Document],
                errorText: String,
                errorMessage: String): Future[InsertOneResult] = {
    val errorDoc = Document("errorText" -> errorText, "errorMessage" -> errorMessage)
    insertDocument(collection, errorDoc)
  }

  def dropDatabase(database: MongoDatabase): Future[Unit] = {
    database.drop().toFuture().map(_ => ())
  }
}
