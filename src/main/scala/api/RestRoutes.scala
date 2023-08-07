package api

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.mongodb.scala.bson.Document

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait RestRoutes extends ThermometerApi {

  protected val thermometers: Route = path(  "thermometers") {
    get {
      val futureData: Future[Seq[Document]] = getThermometers.mapTo[Seq[Document]]

      onComplete(futureData) {
        case Success(data) =>
          val jsonData = data.map(doc => doc.toJson())
          val jsonResponse = "[" + jsonData.mkString(",") + "]"
          complete(HttpEntity(ContentTypes.`application/json`, jsonResponse))

        case Failure(ex) =>
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Error: ${ex.getMessage}"))
      }
    }
  }

  val route: Route = thermometers

}
