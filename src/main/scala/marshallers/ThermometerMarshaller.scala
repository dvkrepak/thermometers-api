package marshallers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import messages.MongoMessages.ThermometerEditor
import play.api.libs.json._
import simulators.Thermometer

import java.util.Date

trait ThermometerMarshaller extends PlayJsonSupport {
  // Human-readable representation of ISO 8601 date format
  implicit val dateReads: Reads[Date] = Reads.dateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val dateWrites: Writes[Date] = Writes.dateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  // Configure JSON serialization
  implicit val thermometerFormat: OFormat[Thermometer] = Json.format[Thermometer]
  implicit val thermometerEditorFormat: OFormat[ThermometerEditor] = Json.format[ThermometerEditor]
}
