package messages

import simulators.Thermometer

object MongoMessages {

  // Operations with Thermometer(-s)
  case class FindThermometersWithPagination(page: Int, pageSize: Int)
  case class CreateThermometer(json: String)
  case class UpdateThermometer(_id: String, json: String)
  case class FindThermometer(_id: String)
  case class DeleteThermometer(_id: String)
  case object CountThermometers

  // Operation with reports
  case class CreateReport(json: String)
  case class FindReportWithRangeWithId(thermometerId: String, createdAtMin: String, createdAtMax: String)
  case class FindReportSummarizedWithPagination(page: Int, pageSize: Int)
  case object CountSummarizedReports

  // Statistic operations
  case class FindMinimumFromReportsWithRange(createdAtMin: String, createdAtMax: String)
  case class FindMaximumFromReportsWithRange(createdAtMin: String, createdAtMax: String)
  case class FindAverageFromReportsWithRange(createdAtMin: String, createdAtMax: String)
  case class FindMedianFromReportsWithRange(createdAtMin: String, createdAtMax: String)

  // Thermometer helper messages
  case class ThermometerEditor(thermometerId: String, data: Thermometer)

}

