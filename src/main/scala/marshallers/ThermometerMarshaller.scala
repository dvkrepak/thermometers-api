package marshallers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import messages.MongoMessages.ThermometerEditor
import play.api.libs.json._
import simulators.Thermometer

trait ThermometerMarshaller extends PlayJsonSupport
                              with ObjectIdMarshaller
                              with DateMarshaller {

  implicit val thermometerFormat: OFormat[Thermometer] = Json.format[Thermometer]
  implicit val thermometerEditorFormat: OFormat[ThermometerEditor] = Json.format[ThermometerEditor]

}
