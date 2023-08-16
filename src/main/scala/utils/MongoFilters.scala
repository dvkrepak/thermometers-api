package utils

import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters.{and, equal, gte, lte}

object MongoFilters {

  def idFilter(_id: String): Bson = {
    Filters.eq("_id", BsonObjectId(_id))
  }

  def thermometerIdWithDateRangeFilter(thermometerId: String, createdAtMin: String, createdAtMax: String): Bson = {

    and(
      equal("thermometerId", BsonObjectId(thermometerId)),
      dateRangeFilter(createdAtMin, createdAtMax)
    )
  }

  def thermometerIdFilter(thermometerId: String): Bson = {
    Filters.eq("thermometerId", BsonObjectId(thermometerId))
  }

  def dateRangeFilter(createdAtMin: String,
                      createdAtMax: String): Bson = {
    and(
      gte("created_at", createdAtMin),
      lte("created_at", createdAtMax)
    )
  }
}
