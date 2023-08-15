package api

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import marshallers.ThermometerMarshaller
import messages.MongoMessages.ThermometerEditor
import org.mongodb.scala.bson.{BsonObjectId, Document}
import org.mongodb.scala.result.UpdateResult
import play.api.libs.json.Json
import simulators.Thermometer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


trait RestRoutes extends ThermometerApi with ThermometerMarshaller {
  val api = "api"
  val version = "v1"
  private val service = "thermometers"

  private val getThermometersList: Route = pathPrefix(api / version / service / "list") {
    get {
      // GET api/v1/thermometers/list
      pathEndOrSingleSlash {
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
  }

  private val getThermometerDetail: Route = pathPrefix(api / version / service / Segment) { _id =>
    get {
      // GET api/v1/thermometers/{_id: String}
      pathEndOrSingleSlash {
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
  }

  private val createThermometer: Route = pathPrefix(api / version / service / "create") {
    post {
      // POST api/v1/thermometers/create
      pathEndOrSingleSlash {
        entity(as[Thermometer]) { thermometer => {

          val withDefaultThermometer: Thermometer = Thermometer.withDefaultCreated(thermometer)
          val futureResponse: Future[BsonObjectId] =
            createThermometer(Json.toJson(withDefaultThermometer).toString())

          handleBasicResponse(futureResponse)
          }
        }
      }
    }
  }

  private val updateThermometer: Route = pathPrefix(api / version / service / "update") {
    patch {
      // PATCH api/v1/thermometers/update
      pathEndOrSingleSlash {
        entity(as[ThermometerEditor]) { editor => {
          val data = Json.toJson(Thermometer.setEditedAt(editor.data))
          val futureResponse: Future[UpdateResult] =
            editThermometer(editor.thermometerId, data.toString())

          handleBasicResponse(futureResponse)
          }
        }
      }
    }
  }


  private val deleteThermometer: Route = pathPrefix(api / version / service / Segment) { _id =>
    delete {
      // DELETE api/v1/thermometers/delete/{_id: String}
      pathEndOrSingleSlash {
        val futureResponse: Future[Long] = deleteThermometer(_id)

        handleBasicResponse(futureResponse)
      }
    }
  }
  private val getDataWithRangeDetail: Route = pathPrefix(api / version / "data" / Segment) { thermometerId =>
    get {
      pathEndOrSingleSlash {
        parameters("createdAtMin".as[String], "createdAtMax".as[String]) { (createdAtMin, createdAtMax) =>

          val futureResponse: Future[Seq[Document]] =
            Try(findDataWithRangeWithId(thermometerId, createdAtMin, createdAtMax)) match {
              case Success(future) => future
              case Failure(e: IllegalArgumentException) =>
                Future.failed(e)
            }

          handleBasicJsonResponse(futureResponse)
        }
      }
    }
  }

  private val getDataDetail: Route = pathPrefix(api / version / "data" / Segment) { thermometerId =>
    get {
      pathEndOrSingleSlash {
        val futureResponse: Future[Seq[Document]] =
          findDataWithId(thermometerId)

        handleBasicJsonResponse(futureResponse)
        }
      }
    }

  private def handleBasicJsonResponse(futureResponse: Future[Seq[Document]]): RequestContext => Future[RouteResult] = {
    onComplete(futureResponse) {
      case Success(data) =>
        val resultJson = data.map(_.toJson)
        complete(HttpEntity(ContentTypes.`application/json`, resultJson.toString))
      case Failure(ex) =>
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Error: ${ex.getMessage}"))
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

  val route: Route = getThermometersList ~ getThermometerDetail ~ createThermometer ~
    updateThermometer ~ deleteThermometer ~ getDataWithRangeDetail ~ getDataDetail
}
