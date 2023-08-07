package actors

import akka.actor.Actor
import akka.event.Logging
import org.mongodb.scala.{MongoClient, MongoDatabase}
import messages.MongoMessages.FindAllThermometers
import org.mongodb.scala.bson.Document

import scala.util.{Try, Success, Failure}

class MongoActor(connectionString: String = "mongodb://localhost:27017",
                 databaseName: String = "optimsys-db")
    extends Actor {
  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val database: MongoDatabase = mongoClient.getDatabase(databaseName)
  private val log = Logging(context.system, this)


  def receive: Receive = {
    case FindAllThermometers => {}
      val senderRef = sender()
      val collection = Try(database.getCollection("thermometers")) match {
        case Success(value) => value
        case Failure(ex) =>
          throw ex
      }
      collection.find().subscribe(
        (document: Document) => {
          senderRef ! document.toJson()
          log.info(s"Completed ${document.toJson()}")
        },
        (error: Throwable) => log.error(s"Error occurred during query: ${error.getMessage}"),
        () => log.info("Query completed.")
      )
  }
}
