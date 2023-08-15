package messages

import org.mongodb.scala.bson.ObjectId

import java.util.Date
import simulators.Thermometer

object MongoMessages {

  // Operations with Thermometer(-s)
  case object FindAllThermometers
  case class CreateThermometer(json: String)
  case class UpdateThermometer(_id: String, json: String)
  case class FindThermometer(_id: String)
  case class DeleteThermometer(_id: String)

  // Statistics operation
  case class CreateData(json: String)
  case class FindDataWithRangeWithId(thermometerId: String, createdAtMin: String, createdAtMax: String)
  case class FindDataWithId(thermometerId: String)

  // Thermometer helper messages
  case class ThermometerEditor(thermometerId: String, data: Thermometer)

}

