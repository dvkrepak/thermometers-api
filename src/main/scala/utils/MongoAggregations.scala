package utils

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Accumulators.first
import com.mongodb.client.model.Aggregates.{group, limit, skip, sort}
import com.mongodb.client.model.Indexes.{ascending, descending}
import org.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonInt32, Document}
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Projections.{computed, excludeId, fields, include}
import org.mongodb.scala.model.{Aggregates, BsonField}


object MongoAggregations {

  private val summaryAggregation: Seq[Bson] = List(
    sort(descending("created_at")),
    group("$thermometerId",
      first("lastTemperature", "$temperature"),
      first("lastEventTime", "$created_at")
    ),
      project(
      fields(
        excludeId(),
        computed("thermometerId", "$_id"),
        include("lastTemperature"),
        include("lastEventTime")
      )
    )
  )

  def summaryAggregationWithPagination(page: Int, pageSize: Int): Seq[Bson] = {
    val paginationStages = List(
      skip((page - 1) * pageSize),
      limit(pageSize)
    )

    summaryAggregation ++ paginationStages
  }

  private def accumulatorWithDateRange(dateFilter: Bson, accumulator: BsonField,
                                       accumulatorName: String): Seq[Bson] = {
    List(
      Aggregates.filter(dateFilter),
      group("$thermometerId", accumulator),
      // Project the output fields
      project(
        fields(
          excludeId(),
          computed("thermometerId", "$_id"),
          include(accumulatorName)
        )
      )
    )
  }
  def minimumDataWithRange(dateFilter: Bson): Seq[Bson] = {
    val accumulator = Accumulators.min("minTemperature", "$temperature")

    accumulatorWithDateRange(dateFilter, accumulator, "minTemperature")
  }

  def maximumDateWithRange(dateFilter: Bson): Seq[Bson] = {
    val accumulator = Accumulators.max("maxTemperature", "$temperature")

    accumulatorWithDateRange(dateFilter, accumulator, "maxTemperature")
  }

  def averageDateWithRange(dateFilter: Bson): Seq[Bson] = {

    val accumulator = Seq(
      Aggregates.filter(dateFilter),
      group("$thermometerId", Accumulators.avg("avgTemperature", "$temperature")),
      // Round the average to 4 decimal places
      project(
        fields(
          excludeId(),
          computed("thermometerId", "$_id"),
          computed("avgTemperature", Document("$round" -> BsonArray("$avgTemperature", BsonInt32(4))))
        )
      )
    )

    accumulator
  }

  def medianDateWithRange(dateFilter: Bson): Seq[Bson] = {

    val aggregation = Seq(
      // Filter the documents by date range
      Aggregates.`match`(dateFilter),
      // Sort the temperatures ascending
      Aggregates.sort(ascending("temperature")),
      // Group by thermometerId and push all the temperatures to an array
      Aggregates.group("$thermometerId", Accumulators.push("temperatures", "$temperature")),
      // Get the size of the array
      Aggregates.project(
        fields(
          computed("thermometerId", "$_id"),
          computed("temperatures", "$temperatures"),
          computed("size", Document("$size" -> "$temperatures"))
        )
      ),
      // Get the median by dividing the size of the array by 2 and rounding it down
      Aggregates.project(
        fields(
          excludeId(),
          computed("thermometerId", "$thermometerId"),
          computed("median",
            Document("$arrayElemAt" ->
              BsonArray("$temperatures",
                BsonDocument("$floor" -> BsonDocument("$divide" -> BsonArray("$size", BsonInt32(2)))))))
        )
      )
    )
    aggregation
  }
}
