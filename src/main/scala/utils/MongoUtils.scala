package utils

import org.bson.conversions.Bson
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}

import scala.annotation.unused
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object MongoUtils {


  private def findCollectionObjectsWithFilter(collection: MongoCollection[Document],
                                              filter: Bson): Future[Seq[Document]] = {
    collection.find(filter).toFuture()
  }

  private def findCollectionObjectsWithAggregation(collection: MongoCollection[Document],
                                                    aggregation: Seq[Bson]): Future[Seq[Document]] = {
    collection.aggregate(aggregation).toFuture()
  }

  private def insertCollectionObject(collection: MongoCollection[Document],
                                     obj: Document): Future[InsertOneResult] = {

    collection
      .insertOne(obj)
      .toFuture()
  }

  private def updateCollectionObject(collection: MongoCollection[Document],
                                     filter: Bson,
                                     update: Bson): Future[UpdateResult] = {
    collection
      .updateOne(filter, update)
      .toFuture()
  }


  private def deleteCollectionObject(collection: MongoCollection[Document],
                                     filter: Bson): Future[DeleteResult] = {
    collection
      .deleteOne(filter)
      .toFuture()
  }

  @unused
  def findCollectionObjects(collection: MongoCollection[Document]): Future[Seq[Document]] = {
    collection.find().toFuture()
  }

  def findCollectionObjectsWithPagination(collection: MongoCollection[Document],
                                          page: Int,
                                          pageSize: Int): Future[Seq[Document]] = {
    val offset = (page - 1) * pageSize

    collection.find().skip(offset).limit(pageSize).toFuture()
  }

  def countCollectionObjects(collection: MongoCollection[Document]): Future[Long] = {
    collection.countDocuments().toFuture()
  }

  def findCollectionObjectWithId(collection: MongoCollection[Document], id: String): Future[Seq[Document]] = {
    val filter = MongoFilters.idFilter(id)
    findCollectionObjectsWithFilter(collection, filter)
  }

  def deleteCollectionObject(collection: MongoCollection[Document], id: String): Future[DeleteResult] = {
    val filter = MongoFilters.idFilter(id)
    deleteCollectionObject(collection, filter)
  }

  def createCollectionObject(collection: MongoCollection[Document], obj: String): Future[InsertOneResult] = {
    val doc = Document(obj)
    insertCollectionObject(collection, doc)
  }

  def updateCollectionObject(collection: MongoCollection[Document], id: String, data: String): Future[UpdateResult] = {
    val filter = MongoFilters.idFilter(id)
    val update = Document("$set" -> Document(data))
    updateCollectionObject(collection, filter, update)
  }

  def findWithDateRangeWithThermometerId(collection: MongoCollection[Document],
                                          id: String,
                                          createdAtMin: String,
                                          createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.thermometerIdWithDateRangeFilter(id, createdAtMin, createdAtMax)

    findCollectionObjectsWithFilter(collection, filter)
  }

  def findSummarizedWithPagination(collection: MongoCollection[Document],
                                   page: Int,
                                   pageSize: Int): Future[Seq[Document]] = {
    val aggregation = MongoAggregations.summaryAggregationWithPagination(page, pageSize)

    findCollectionObjectsWithAggregation(collection, aggregation)
  }

  def countSummarizedReports(collection: MongoCollection[Document]): Future[Long] = {
    // Count unique thermometerId values
    val aggregation = MongoAggregations.thermometerIdGroup

    findCollectionObjectsWithAggregation(collection, aggregation)
      .map(_.length.asInstanceOf[Long])
  }

  def findMinimumDataWithRange(collection: MongoCollection[Document],
                               createdAtMin: String,
                               createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.minimumDataWithRange(filter)

    findCollectionObjectsWithAggregation(collection, aggregation)
  }

  def findMaximumFromReportsWithRange(collection: MongoCollection[Document],
                                      createdAtMin: String,
                                      createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.maximumDateWithRange(filter)

    findCollectionObjectsWithAggregation(collection, aggregation)
  }

  def findAverageFromReportsWithRange(collection: MongoCollection[Document],
                                      createdAtMin: String,
                                      createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.averageDateWithRange(filter)

    findCollectionObjectsWithAggregation(collection, aggregation)
  }

  def findMedianFromReportsWithRange(collection: MongoCollection[Document],
                                     createdAtMin: String,
                                     createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.dateRangeFilter(createdAtMin, createdAtMax)
    val aggregation = MongoAggregations.medianDateWithRange(filter)

    findCollectionObjectsWithAggregation(collection, aggregation)
  }

  def saveError(collection: MongoCollection[Document],
                errorText: String,
                error: String): Future[InsertOneResult] = {
    val doc = Document("error" -> errorText, "message" -> error)
    insertCollectionObject(collection, doc)
  }

  def dropDatabase(collection: MongoDatabase): Future[Unit] = {
    collection.drop().toFuture().map(_ => ())
  }
}
