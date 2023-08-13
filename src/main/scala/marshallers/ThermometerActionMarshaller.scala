package marshallers

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.{Json, JsonConfiguration, OFormat, OptionHandlers}
import simulators.ThermometerAction

trait ThermometerActionMarshaller extends ThermometerMarshaller
                                    with ObjectIdMarshaller
                                    with DateMarshaller {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)
  implicit val thermometerActionFormat: OFormat[ThermometerAction] = Json.format[ThermometerAction]
}
