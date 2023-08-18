package utils

import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
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

}
