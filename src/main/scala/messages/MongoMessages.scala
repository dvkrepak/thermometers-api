package messages

object MongoMessages {

  case object FindAllThermometers

  case class CreateThermometer(jsonData: String)
}
