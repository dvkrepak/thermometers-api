package messages

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

  // Thermometer helper messages
  case class ThermometerEditor(thermometerId: String, data: Thermometer)
}

