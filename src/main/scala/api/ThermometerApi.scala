package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import messages.MongoMessages.{CreateThermometer, EditThermometer, FindAllThermometers, FindThermometer}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.{InsertOneResult, UpdateResult}

import scala.concurrent.Future

trait ThermometerApi {

  def createMongoDbActor(): ActorRef

  implicit val requestTimeout: Timeout
  private lazy val mongoActor: ActorRef = createMongoDbActor()

  protected def getThermometers: Future[Seq[Document]] = {
    (mongoActor ? FindAllThermometers).mapTo[Seq[Document]]
  }

  protected def createThermometer(jsonString: String): Future[InsertOneResult] = {
    (mongoActor ? CreateThermometer(jsonString)).mapTo[InsertOneResult]
  }

  protected def editThermometer(_id: String, json: String): Future[UpdateResult] = {
    (mongoActor ? EditThermometer(_id, json)).mapTo[UpdateResult]
  }

  protected def findThermometer(_id: String): Future[Option[Document]] = {
    (mongoActor ? FindThermometer(_id)).mapTo[Option[Document]]
  }

}
