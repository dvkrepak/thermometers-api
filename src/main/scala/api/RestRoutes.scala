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

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


trait RestRoutes extends ThermometerApi with ThermometerMarshaller {
  val api = "api"
  val version = "v1"
  private val service = "thermometers"

  private val getThermometersList: Route = pathPrefix(api / version / service / "list") {
    get {
      // GET api/v1/thermometers/list
      pathEndOrSingleSlash {
        val futureResponse = getThermometers.mapTo[Seq[Document]]

        onComplete(futureResponse) {
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
        lazy val lazyResponse: Future[Option[Document]] = findThermometer(_id)
        val futureResponse = withValidation(lazyResponse).mapTo[Option[Document]]

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
          lazy val lazyResponse: Future[UpdateResult] =
            editThermometer(editor.thermometerId, data.toString())
          val futureResponse = withValidation(lazyResponse).mapTo[UpdateResult]

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
        lazy val lazyResponse: Future[Long] = deleteThermometer(_id)
        val futureResponse = withValidation(lazyResponse).mapTo[Long]

        handleBasicResponse(futureResponse)
      }
    }
  }

  private val getDataSummarizedList: Route = pathPrefix(api / version / service / "data" / "list") {
    get {
      // GET api/v1/thermometers/data/list
      pathEndOrSingleSlash {
        val futureResponse: Future[Seq[Document]] = findDataSummarized()

        handleBasicJsonResponse(futureResponse)
      }
    }
  }

  private val getDataWithRangeDetail: Route = pathPrefix(api / version / service / "data" / Segment) { thermometerId =>
    get {
      // GET api/v1/thermometers/data/{_id: String}/?createdAtMin={Date}&createdAtMax={Date}
      pathEndOrSingleSlash {
        parameters("createdAtMin".as[String], "createdAtMax".as[String]) { (createdAtMin, createdAtMax) =>

          lazy val lazyResponse: Future[Seq[Document]] = findDataWithRangeWithId(thermometerId, createdAtMin, createdAtMax)
          val futureResponse = withValidation(lazyResponse).mapTo[Seq[Document]]

          handleBasicJsonResponse(futureResponse)
        }
      }
    }
  }

  private val getDataDetail: Route = pathPrefix(api / version / service / "data" / Segment) { thermometerId =>
    get {
      // GET api/v1/thermometers/data/{_id: String}
      pathEndOrSingleSlash {
        lazy val lazyResponse: Future[Seq[Document]] = findDataWithId(thermometerId)
        val futureResponse = withValidation(lazyResponse).mapTo[Seq[Document]]

        handleBasicJsonResponse(futureResponse)
        }
      }
    }

  private def withValidation(futureResponse: => Future[Any]): Future[Any] = {
    Try(futureResponse) match {
      case Success(future) => future
      case Failure(e: IllegalArgumentException) =>
        Future.failed(e)
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
    updateThermometer ~ deleteThermometer ~ getDataSummarizedList ~
    getDataWithRangeDetail ~ getDataDetail
}
