package marshallers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json._
import simulators.Thermometer

import java.util.Date

trait ThermometerMarshaller extends PlayJsonSupport {
  // Human-readable representation of ISO 8601 date format
  implicit val dateReads: Reads[Date] = Reads.dateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val dateWrites: Writes[Date] = Writes.dateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  implicit val thermometerFormat: OFormat[Thermometer] = Json.format[Thermometer]

  // Configure JSON serialization
  // Any None values will be serialized as explicit JSON nulls
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)
}
