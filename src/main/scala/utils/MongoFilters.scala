package utils

import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters

object MongoFilters {

  def idFilter(_id: String): Bson = {
    Filters.eq("_id", BsonObjectId(_id))
  }

}
