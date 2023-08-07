package routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}

import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import org.mongodb.scala.bson.Document

import scala.concurrent.Future
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import messages.MongoMessages.FindAllThermometers

import scala.concurrent.duration.DurationInt

class RestRoutes(mongoActor: ActorRef) {
  implicit val timeout: Timeout = Timeout(5.seconds)

  val route: Route = path(  "hello-world") {
    get {
      val futureData = (mongoActor ? FindAllThermometers).mapTo[String]

      onComplete(futureData) {
        case Success(data) =>
          complete(HttpEntity(ContentTypes.`application/json`, data))

        case Failure(ex) =>
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Error: ${ex.getMessage}"))
      }
    }
  }
}
