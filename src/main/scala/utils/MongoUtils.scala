package utils

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{BsonObjectId, Document}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}

import scala.concurrent.Future

object MongoUtils {

  def findCollectionObjects(collection: MongoCollection[Document]): Future[Seq[Document]] = {
    collection.find().toFuture()
  }

  def findCollectionObject(collection: MongoCollection[Document], _id: String): Future[Option[Document]] = {
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
    val filter = Filters.eq("_id", BsonObjectId(_id))
    val update = Document("$set" -> Document(data))
    collection
      .updateOne(filter, update)
      .toFuture()
  }
}
