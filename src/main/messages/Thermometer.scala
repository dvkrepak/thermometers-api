package messages

object Thermometer {

  sealed trait ThermometerOperation
  // TODO: Add return types
  case class CreateThermometer(description: String) extends ThermometerOperation
  case class ReadThermometer(id: String) extends ThermometerOperation
  // History per specified time window
  case class ReadWithTimeThermometer(id: String, time: Int) extends ThermometerOperation

  // TODO: Add 'TYPE of argument' to UpdateThermometer - JSON
  case class UpdateThermometer(newInformation: Any) extends ThermometerOperation
  case class DeleteThermometer() extends ThermometerOperation
  case class ReadThermometers() // No extension


  sealed trait ThermometerStatistics
  // TODO: Finish statistics
}
