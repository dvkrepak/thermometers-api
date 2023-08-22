package marshallers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import messages.MongoMessages.ThermometerEditor
import play.api.libs.json.{Json, OFormat}
import simulators.Thermometer

/**
 * A trait that provides JSON marshalling support for `Thermometer` and `ThermometerEditor` messages
 */
trait ThermometerMarshaller extends PlayJsonSupport with ObjectIdMarshaller with DateMarshaller {

  // Implicit JSON format for serializing and deserializing Thermometer objects
  implicit val thermometerFormat: OFormat[Thermometer] = Json.format[Thermometer]

  // Implicit JSON format for serializing and deserializing ThermometerEditor messages
  implicit val thermometerEditorFormat: OFormat[ThermometerEditor] = Json.format[ThermometerEditor]
}
