package messages

import simulators.Thermometer

object MongoMessages {

  // Operations with Thermometer(-s)
  case object FindAllThermometers
  case class CreateThermometer(json: String)
  case class EditThermometer(_id: String, json: String)
  case class FindThermometer(_id: String)


  case class ThermometerEditor(_id: String, data: Thermometer)

}

