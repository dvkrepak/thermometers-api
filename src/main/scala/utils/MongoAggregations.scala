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

  private val baseSummaryAggregation: Seq[Bson] = List(
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

  val thermometerIdGroup: Seq[Bson] = Seq(
    group("$thermometerId"),
  )

  /**
   * Aggregation for summary with pagination.
   *
   * @param page Page number
   * @param pageSize Number of items per page
   * @return Aggregation sequence with pagination stages
   */
  def summaryAggregationWithPagination(page: Int, pageSize: Int): Seq[Bson] = {
    val paginationStages = List(
      skip((page - 1) * pageSize),
      limit(pageSize)
    )

    baseSummaryAggregation ++ paginationStages
  }

  private def accumulatorWithDateRange(dateFilter: Bson, accumulator: BsonField, accumulatorName: String): Seq[Bson] = {
    val filterStage = Aggregates.filter(dateFilter)
    val groupStage = group("$thermometerId", accumulator)

    val projectStage = project(
      fields(
        excludeId(),
        computed("thermometerId", "$_id"),
        include(accumulatorName)
      )
    )

    Seq(filterStage, groupStage, projectStage)
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
    val filteredAccumulator = Aggregates.filter(dateFilter)
    val groupedAvgAccumulator = group("$thermometerId",
      Accumulators.avg("avgTemperature", "$temperature"))

    val roundedAvgAccumulator = project(
      fields(
        excludeId(),
        computed("thermometerId", "$_id"),
        computed("avgTemperature",
          Document("$round" -> BsonArray("$avgTemperature", BsonInt32(4))))
      )
    )

    Seq(filteredAccumulator, groupedAvgAccumulator, roundedAvgAccumulator)
  }

  def medianDateWithRange(dateFilter: Bson): Seq[Bson] = {
    // Define individual aggregation stages
    val filterStage = Aggregates.`match`(dateFilter)
    val sortStage = Aggregates.sort(ascending("temperature"))
    val groupStage = Aggregates.group("$thermometerId",
      Accumulators.push("temperatures", "$temperature"))

    val projectSizeStage = Aggregates.project(
      fields(
        computed("thermometerId", "$_id"),
        computed("temperatures", "$temperatures"),
        computed("size", Document("$size" -> "$temperatures"))
      )
    )

    val projectMedianStage = Aggregates.project(
      fields(
        excludeId(),
        computed("thermometerId", "$thermometerId"),
        computed("median",
          Document("$arrayElemAt" ->
            BsonArray("$temperatures",
              BsonDocument("$floor" -> BsonDocument("$divide" -> BsonArray("$size", BsonInt32(2)))))))
      )
    )

    Seq(filterStage, sortStage, groupStage, projectSizeStage, projectMedianStage)
  }
}
