package marshallers

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.{Json, JsonConfiguration, OFormat, OptionHandlers}
import simulators.ThermometerAction

/**
 * A trait that provides JSON marshalling support for `ThermometerAction`
 */
trait ThermometerActionMarshaller extends ThermometerMarshaller with ObjectIdMarshaller with DateMarshaller {

  // Implicit JSON config for writing Null values
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)

  // Implicit JSON format for serializing and deserializing ThermometerAction objects
  implicit val thermometerActionFormat: OFormat[ThermometerAction] = Json.format[ThermometerAction]
}
