package messages

import simulators.Thermometer

object MongoMessages {

  // Operations with Thermometer(-s)
  case object FindAllThermometers
  case class CreateThermometer(json: String)
  case class UpdateThermometer(_id: String, json: String)
  case class FindThermometer(_id: String)
  case class DeleteThermometer(_id: String)

  // Operation with reports
  case class CreateReport(json: String)
  case class FindReportWithRangeWithId(thermometerId: String, createdAtMin: String, createdAtMax: String)
  case class FindReportWithId(thermometerId: String)
  object FindReportsSummarized

  // Statistic operations
  case class FindMinimumFromReportsWithRange(createdAtMin: String, createdAtMax: String)
  case class FindMaximumFromReportsWithRange(createdAtMin: String, createdAtMax: String)
  case class FindAverageFromReportsWithRange(createdAtMin: String, createdAtMax: String)
  case class FindMedianFromReportsWithRange(createdAtMin: String, createdAtMax: String)

  // Thermometer helper messages
  case class ThermometerEditor(thermometerId: String, data: Thermometer)

}

