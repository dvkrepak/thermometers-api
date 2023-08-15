package utils

import com.mongodb.client.model.Accumulators.first
import com.mongodb.client.model.Aggregates.{group, sort}
import com.mongodb.client.model.Indexes.descending
import org.bson.conversions.Bson

object MongoAggregations {

  val summaryAggregation: Seq[Bson] = List(
    sort(descending("created_at")),
    group("$thermometerId",
      first("lastTemperature", "$temperature"),
      first("lastEventTime", "$created_at"))
  )
}
