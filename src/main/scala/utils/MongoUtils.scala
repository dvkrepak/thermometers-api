package utils

import akka.http.scaladsl.server.Directives.onComplete
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success


object MongoUtils {

  private def findCollectionObjectsWithFilter(collection: MongoCollection[Document],
                                              filter: Bson): Future[Seq[Document]] = {
    collection.find(filter).toFuture()
  }

  private def findCollectionObjectsWithAggregation(collection: MongoCollection[Document],
                                                    aggregation: Seq[Bson]): Future[Seq[Document]] = {
    collection.aggregate(aggregation).toFuture()
  }

  def findCollectionObjects(collection: MongoCollection[Document]): Future[Seq[Document]] = {
    collection.find().toFuture()
  }

  def findCollectionObjectsWithPagination(collection: MongoCollection[Document],
                                          page: Int,
                                          pageSize: Int): Future[Seq[Document]] = {
    val offset = (page - 1) * pageSize

    collection.find().skip(offset).limit(pageSize).toFuture()
  }

  def findCollectionObjectWithId(collection: MongoCollection[Document], _id: String): Future[Seq[Document]] = {
    val filter = MongoFilters.idFilter(_id)
    findCollectionObjectsWithFilter(collection, filter)
  }

  def deleteCollectionObject(collection: MongoCollection[Document], _id: String): Future[DeleteResult] = {
    val filter = MongoFilters.idFilter(_id)
    collection
      .deleteOne(filter)
      .toFuture()
  }

  def createCollectionObject(collection: MongoCollection[Document], obj: String): Future[InsertOneResult] = {
    val doc = Document(obj)
    collection
      .insertOne(doc)
      .toFuture()
  }

  def updateCollectionObject(collection: MongoCollection[Document], _id: String, data: String): Future[UpdateResult] = {
    val filter = MongoFilters.idFilter(_id)
    val update = Document("$set" -> Document(data))
    collection
      .updateOne(filter, update)
      .toFuture()
  }

  def findWithDateRangeWithThermometerId(collection: MongoCollection[Document],
                                          _id: String,
                                          createdAtMin: String,
                                          createdAtMax: String): Future[Seq[Document]] = {
    val filter = MongoFilters.thermometerIdWithDateRangeFilter(_id, createdAtMin, createdAtMax)

    findCollectionObjectsWithFilter(collection, filter)
  }

  def findSummarizedWithPagination(collection: MongoCollection[Document],
                                   page: Int,
                                   pageSize: Int): Future[Seq[Document]] = {
    val aggregation = MongoAggregations.summaryAggregationWithPagination(page, pageSize)

    findCollectionObjectsWithAggregation(collection, aggregation)
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

}
