package actors

import akka.actor.Actor
import akka.event.Logging
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.InsertOneResult
import messages.MongoMessages._

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

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
      val documentsBuffer = ListBuffer.empty[Document]

      collection.find().subscribe(
        (document: Document) => {
          log.info(s"Find Document: ${document.toJson}")
          documentsBuffer += document
        },
        (error: Throwable) => log.error(s"Error occurred during query: ${error.getMessage}"),
        () => {
          senderRef ! documentsBuffer.toList
          log.info("Query completed")
        }
      )

    case CreateThermometer(thermometer: String) =>
      val senderRef = sender()
      val doc: Document = Document(thermometer)
      database.getCollection("thermometers").insertOne(doc).subscribe(
        (respond: InsertOneResult) => {
          senderRef ! respond
          log.info(s"InsertedOne Document: $respond")
        },
        (error: Throwable) => log.error(s"Error occurred during InsertOne: ${error.getMessage}"),
        () => {
          log.info("InsertOne Completed")
        }
      )
  }
}
