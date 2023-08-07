package messages

object MongoMessages {
  case class InitDatabase(connectionString: String, databaseName: String)

  case object FindAllThermometers
}
