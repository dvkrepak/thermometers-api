package utils

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Accumulators.first
import com.mongodb.client.model.Aggregates.{group, sort}
import com.mongodb.client.model.Indexes.descending
import org.bson.conversions.Bson
import org.mongodb.scala.model.{Aggregates, BsonField}
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Projections.{computed, excludeId, fields, include}

object MongoAggregations {

  val summaryAggregation: Seq[Bson] = List(
    sort(descending("created_at")),
    group("$thermometerId",
      first("lastTemperature", "$temperature"),
      first("lastEventTime", "$created_at")
    )
  )

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
    val accumulator = Accumulators.avg("avgTemperature", "$temperature")

    accumulatorWithDateRange(dateFilter, accumulator, "avgTemperature")
  }
}
