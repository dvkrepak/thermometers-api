package api

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import marshallers.ThermometerMarshaller
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.InsertOneResult
import play.api.libs.json.Json
import simulators.Thermometer

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait RestRoutes extends ThermometerApi with ThermometerMarshaller {

  private val getAllThermometers: Route = path(  "thermometers") {
    get {
      val futureData: Future[Seq[Document]] = getThermometers.mapTo[Seq[Document]]

      onComplete(futureData) {
        case Success(data) =>
            val jsonData = data.map(_.toJson)
            val jsonResponse = "[" + jsonData.mkString(",") + "]"
            complete(HttpEntity(ContentTypes.`application/json`, jsonResponse))
        case Failure(ex) =>
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Error: ${ex.getMessage}"))
      }
    }
  }

  private val postOneThermometer: Route = path("thermometer") {
    post {
      entity(as[Thermometer]) {thermometer => {

        val withDefaultThermometer = Thermometer.withDefaultCreated(thermometer)
        val futureRespond: Future[InsertOneResult] =
          createThermometer(Json.toJson(withDefaultThermometer).toString())

        onComplete(futureRespond) {
          case Success(data) =>
            complete(StatusCodes.OK, data.toString)
          case Failure(ex) =>
            complete(StatusCodes.InternalServerError, s"Error: ${ex.getMessage}")
          }
        }
      }
    }
  }

  val route: Route = getAllThermometers ~ postOneThermometer
}
