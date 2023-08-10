package api

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import marshallers.ThermometerMarshaller
import messages.MongoMessages.ThermometerEditor
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}
import play.api.libs.json.Json
import simulators.Thermometer

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait RestRoutes extends ThermometerApi with ThermometerMarshaller {

  private val getAllThermometers: Route = path("thermometers") {
    get {
      // GET host/thermometers
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
      // POST host/thermometer/:thermometer
      entity(as[Thermometer]) { thermometer => {

        val withDefaultThermometer: Thermometer = Thermometer.withDefaultCreated(thermometer)
        val futureResponse: Future[InsertOneResult] =
          createThermometer(Json.toJson(withDefaultThermometer).toString())

        handleBasicResponse(futureResponse)
      }
      }
    }
  }

  private val editThermometer: Route = path("edit" / "thermometer") {
    post {
      // POST host/edit/thermometer/:thermometer
      entity(as[ThermometerEditor]) { editor => {
        val data = Json.toJson(Thermometer.setEditedAt(editor.data))
        val futureResponse: Future[UpdateResult] =
          editThermometer(editor._id, data.toString())

        handleBasicResponse(futureResponse)
        }
      }
    }
  }

  private val getThermometer: Route = path("thermometer" / Segment) { _id =>
    get {
      // GET host/thermometer/:_id
      val futureResponse: Future[Option[Document]] = findThermometer(_id)

      onComplete(futureResponse) {
        case Success(data) =>
          val resultJson = data.map(_.toJson).getOrElse(Json.toJson(None))
          complete(HttpEntity(ContentTypes.`application/json`, resultJson.toString))
        case Failure(ex) =>
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Error: ${ex.getMessage}"))
      }
    }
  }

  private val postDeleteThermometer: Route = path("delete" / "thermometer" / Segment) { _id =>
    delete {
      val futureResponse: Future[DeleteResult] = deleteThermometer(_id)
      handleBasicResponse(futureResponse)
    }
  }

  private def handleBasicResponse(futureResponse: Future[Any]): RequestContext => Future[RouteResult] = {
    onComplete(futureResponse) {
      case Success(data) =>
        complete(StatusCodes.OK, data.toString)
      case Failure(ex) =>
        complete(StatusCodes.InternalServerError, s"Error: ${ex.getMessage}")
    }
  }

  val route: Route = getAllThermometers ~ postOneThermometer ~ editThermometer ~ getThermometer ~ postDeleteThermometer
}
