package marshallers

import play.api.libs.json.{Json, OFormat}
import simulators.ThermometerAction

trait ThermometerActionMarshaller extends ThermometerMarshaller
                                    with ObjectIdMarshaller
                                    with DateMarshaller {

  implicit val thermometerActionFormat: OFormat[ThermometerAction] = Json.format[ThermometerAction]
}
