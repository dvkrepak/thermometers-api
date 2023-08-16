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
import utils.RouteUtils

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

        handleBasicJsonResponse(futureResponse)
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

  private val getDataSummarizedList: Route = pathPrefix(api / version / service / "reports" / "list") {
    get {
      // GET api/v1/thermometers/reports/list
      pathEndOrSingleSlash {
        val futureResponse: Future[Seq[Document]] = findReportSummarized()

        handleBasicJsonResponse(futureResponse)
      }
    }
  }

  private val getDataWithRangeDetail: Route = pathPrefix(api / version / service / "reports" / Segment) { thermometerId =>
    get {
      // GET api/v1/thermometers/reports/{_id: String}/?from={Date}&till={Date}
      pathEndOrSingleSlash {
        parameters("from".as[String], "till".as[String]) { (createdAtMin, createdAtMax) =>

          lazy val lazyResponse: Future[Seq[Document]] =
            findReportWithRangeWithId(thermometerId, createdAtMin, createdAtMax)
          val futureResponse = withValidation(lazyResponse).mapTo[Seq[Document]]

          handleBasicJsonResponse(futureResponse)
        }
      }
    }
  }

  private val getStatisticsDataList: Route = pathPrefix(api / version / service / "statistics") {
    get {
      // GET api/v1/thermometers/statistics/?from={Date}&till={Date}&tmp_min={Boolean}
      pathEndOrSingleSlash {
        parameters(
          "from".as[String],
          "till".as[String],
          "tmp_min".as[Boolean].?,
          "tmp_max".as[Boolean].?,
          "tmp_avg".as[Boolean].?,
          "tmp_med".as[Boolean].?
        ) { (createdAtMin, createdAtMax, minimumOpt, maximumOpt, averageOpt, medianOpt) =>

          val operation: Option[RouteUtils.Operation] =
            RouteUtils.convertToOperation(minimumOpt, maximumOpt, averageOpt, medianOpt)

          operation match {
            case Some(selectedOperation) =>

              val functions: Seq[(String, String) => Future[Seq[Document]]] =
                Seq(findMinimumFromReportsWithRange, findMaximumFromReportsWithRange,
                  findAverageFromReportsWithRange)
              val resolver = RouteUtils.buildOperationResolver(functions)
              val function = RouteUtils.getStatisticFunction(selectedOperation, resolver)

              lazy val lazyResponse: Future[Seq[Document]] = function(createdAtMin, createdAtMax)
              val futureResponse = withValidation(lazyResponse).mapTo[Seq[Document]]
              handleBasicJsonResponse(futureResponse)

            case None =>
              complete(StatusCodes.BadRequest,
                "Exactly one operation from minimum/maximum/average/median must be selected.")
            }
          }
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
        val jsonResponse = "[" + resultJson.mkString(",") + "]"
        complete(HttpEntity(ContentTypes.`application/json`, jsonResponse))
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

  val route: Route =
    getStatisticsDataList ~
    getDataSummarizedList ~
    getDataWithRangeDetail ~
    getThermometersList ~ getThermometerDetail ~ createThermometer ~
    updateThermometer ~ deleteThermometer


}
