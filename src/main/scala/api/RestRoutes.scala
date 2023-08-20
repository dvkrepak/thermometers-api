package api

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import marshallers.ThermometerMarshaller
import messages.MongoMessages.ThermometerEditor
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}
import play.api.libs.json.{JsArray, Json}
import simulators.Thermometer
import utils.RejectionHandlers.{thermometerEditorRejectionHandler, thermometerRejectionHandler}
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
        parameters("page".as[Int].?(1), "page_size".as[Int].?(10)) { (page, pageSize) =>

          val futureResponse: Future[Seq[Document]] = findThermometersWithPagination(page, pageSize)
          val countFuture: Future[Long] = countThermometers()

          val combinedResponse = futureResponse.zip(countFuture)


          handleBasicJsonPaginationResponse(combinedResponse, page, pageSize)
          }
        }
      }
    }

  private val getThermometerDetail: Route = pathPrefix(api / version / service / Segment) { _id =>
    get {
      // GET api/v1/thermometers/{_id: String}
      pathEndOrSingleSlash {
        lazy val lazyResponse: Future[Seq[Document]] = findThermometer(_id)
        val futureResponse = withValidation(lazyResponse).mapTo[Seq[Document]]

        handleBasicJsonResponse(futureResponse)
      }
    }
  }

  private val createThermometer: Route = handleRejections(thermometerRejectionHandler) {
    pathPrefix(api / version / service / "create") {
      post {
        // POST api/v1/thermometers/create
        pathEndOrSingleSlash {
          entity(as[Thermometer]) { thermometer => {

            val withDefaultThermometer: Thermometer = Thermometer.withDefaultCreated(thermometer)
            val futureResponse: Future[InsertOneResult] =
              createThermometer(Json.toJson(withDefaultThermometer).toString())

            handleBasicResponse(futureResponse)
          }
          }
        }
      }
    }
  }

  private val updateThermometer: Route = handleRejections(thermometerEditorRejectionHandler) {
    pathPrefix(api / version / service / "update") {
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
  }

  private val deleteThermometer: Route = pathPrefix(api / version / service / "delete" / Segment) { _id =>
    delete {
      // DELETE api/v1/thermometers/delete/{_id: String}
      pathEndOrSingleSlash {
        lazy val lazyResponse: Future[DeleteResult] = deleteThermometer(_id)
        val futureResponse = withValidation(lazyResponse).mapTo[DeleteResult]

        handleBasicResponse(futureResponse)
      }
    }
  }

  private val getDataSummarizedList: Route = pathPrefix(api / version / service / "reports" / "list") {
    get {
      // GET api/v1/thermometers/reports/list
      pathEndOrSingleSlash {
        parameters("page".as[Int].?(1), "page_size".as[Int].?(10)) { (page, pageSize) =>

          val futureResponse: Future[Seq[Document]] = findReportSummarizedWithPagination(page, pageSize)
          val countFuture: Future[Long] = countSummarizedReports()

          val combinedResponse = futureResponse.zip(countFuture)

          handleBasicJsonPaginationResponse(combinedResponse, page, pageSize)
        }
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
                  findAverageFromReportsWithRange, findMedianFromReportsWithRange)

              val resolver = RouteUtils.buildOperationResolver(functions)
              val function: Option[(String, String) => Future[Seq[Document]]] =
                RouteUtils.getStatisticFunction(selectedOperation, resolver)

              if (function.isEmpty) {
                complete(StatusCodes.BadRequest, "Error: Invalid operation selected.")
              }

              lazy val lazyResponse: Future[Seq[Document]] = function.get(createdAtMin, createdAtMax)
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
      case Failure(e: Throwable) =>
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
        val errorMessage = s"Error: ${ex.getMessage}"
        complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/plain(UTF-8)`, errorMessage))
    }
  }

  private def handleBasicResponse(futureResponse: Future[Any]): RequestContext => Future[RouteResult] = {
    onComplete(futureResponse) {
      case Success(data) =>
        complete(StatusCodes.OK, data.toString)
      case Failure(ex) =>
        complete(StatusCodes.BadRequest, s"Error: ${ex.getMessage}")
    }
  }

  private def handleBasicJsonPaginationResponse(futureResponse: Future[(Seq[Document], Long)],
                                                page: Int,
                                                pageSize: Int): RequestContext => Future[RouteResult] = {
    onComplete(futureResponse) {
      case Success(response) =>

        val data: Seq[Document] = response._1

        if (data.isEmpty) {
          // If there is no data, return 204 No Content
          complete(StatusCodes.NoContent, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "No content to display"))
        } else {

          val count: Long = response._2 // Count of all documents

          val resultJsonArray: String = data.map(_.toJson).mkString("[", ",", "]") // Convert to JSON array
          val parsedArray: JsArray = Json.parse(resultJsonArray).as[JsArray] // Parse JSON array

          val totalPages: Int = Math.ceil(count.toDouble / pageSize.toDouble).toInt // Calculate total pages
          val hasNextPage: Boolean = page < totalPages

          val jsonData = Json.obj(
            "count" -> count,
            "results" -> parsedArray,
            "page" -> page,
            "page_size" -> pageSize,
            "next_page" -> hasNextPage,
          )

          complete(HttpEntity(ContentTypes.`application/json`, Json.stringify(jsonData)))
        }

      case Failure(ex) =>
        val errorMessage = s"Error: ${ex.getMessage}"
        complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/plain(UTF-8)`, errorMessage))
    }
  }

  val route: Route =
    getStatisticsDataList ~
    getDataSummarizedList ~ getDataWithRangeDetail ~
    getThermometersList ~ getThermometerDetail ~
    createThermometer ~ updateThermometer ~ deleteThermometer
}
