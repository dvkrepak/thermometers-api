package utils

import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}

import scala.concurrent.Future

object MongoUtils {

  private def findCollectionObjectsWithFilter(collection: MongoCollection[Document],
                                              filter: Bson): Future[Seq[Document]] = {
    collection.find(filter).toFuture()
  }

  private def findCollectionObjectsWithAggregation(collection: MongoCollection[Document],
                                                    aggregation: Seq[Bson]) = {
    collection.aggregate(aggregation).toFuture()
  }

  def findCollectionObjects(collection: MongoCollection[Document]): Future[Seq[Document]] = {
    collection.find().toFuture()
  }

  def findCollectionObjectWithId(collection: MongoCollection[Document], _id: String): Future[Option[Document]] = {
    val filter = MongoFilters.idFilter(_id)
    collection
      .find(filter)
      .headOption()
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
      .head()
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

  def findWithThermometerId(collection: MongoCollection[Document],
                            thermometerId: String): Future[Seq[Document]] = {
    val filter = MongoFilters.thermometerIdFilter(thermometerId)
    findCollectionObjectsWithFilter(collection, filter)
  }

  def findSummaries(collection: MongoCollection[Document]): Future[Seq[Document]] = {
    val aggregation = MongoAggregations.summaryAggregation
    findCollectionObjectsWithAggregation(collection, aggregation)
  }
}
