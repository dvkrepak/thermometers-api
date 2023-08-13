package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import messages.MongoMessages._
import org.mongodb.scala.bson.{BsonObjectId, Document}
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.Future

trait ThermometerApi {

  def createMongoDbActor(): ActorRef

  implicit val requestTimeout: Timeout
  private lazy val mongoActor: ActorRef = createMongoDbActor()

  protected def getThermometers: Future[Seq[Document]] = {
    (mongoActor ? FindAllThermometers).mapTo[Seq[Document]]
  }

  protected def createThermometer(jsonString: String): Future[BsonObjectId] = {
    (mongoActor ? CreateThermometer(jsonString)).mapTo[BsonObjectId]
  }

  protected def editThermometer(_id: String, json: String): Future[UpdateResult] = {
    (mongoActor ? EditThermometer(_id, json)).mapTo[UpdateResult]
  }

  protected def findThermometer(_id: String): Future[Option[Document]] = {
    (mongoActor ? FindThermometer(_id)).mapTo[Option[Document]]
  }

  protected def deleteThermometer(_id: String): Future[Long] = {
    (mongoActor ? DeleteThermometer(_id)).mapTo[Long]
  }

  protected def saveData(thermometerAction: String): Future[BsonObjectId] = {
    (mongoActor ? SaveData(thermometerAction)).mapTo[BsonObjectId]
  }
}
