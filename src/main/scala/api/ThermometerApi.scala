package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import messages.MongoMessages.FindAllThermometers
import org.mongodb.scala.bson.Document

import scala.concurrent.Future

trait ThermometerApi {

  def createMongoDbActor(): ActorRef

  implicit val requestTimeout: Timeout

  private lazy val mongoActor: ActorRef = createMongoDbActor()

  protected def getThermometers: Future[Seq[Document]] = {
    (mongoActor ? FindAllThermometers).mapTo[Seq[Document]]
  }
}
