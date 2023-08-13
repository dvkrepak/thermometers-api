package messages

import akka.http.scaladsl.model.headers.Date
import simulators.Thermometer

object MongoMessages {

  // Operations with Thermometer(-s)
  case object FindAllThermometers
  case class CreateThermometer(json: String)
  case class EditThermometer(_id: String, json: String)
  case class FindThermometer(_id: String)
  case class DeleteThermometer(_id: String)

  // Statistics operation
  case class SaveData(json: String)
  case class FindData(_id: String, createdAtMin: Date, createdAtMax: Date)

  // Thermometer helper messages
  case class ThermometerEditor(thermometerId: String, data: Thermometer)
}

