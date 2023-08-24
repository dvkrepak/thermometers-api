package utils

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{RejectionHandler, StandardRoute, ValidationRejection}
import org.mongodb.scala.bson.ObjectId

object RejectionHandlers {

  private def getTypeErrorMessage(fieldName: String, expectedType: String): String = {
    s"Field '$fieldName' received incorrect type. Expected: $expectedType."
  }

  private def getExpectedType(fieldName: String): String = fieldName match {
      case "id" => "ObjectId"
      case "description" => "String"
      case "createdAt" => "Date"
      case "editedAt" => "Date"
      case _ => "Unknown"
    }

  private def getFieldName(message: String): String = {
    // {"obj.<value>":[{"msg":["error.expected.<expected>"],"args":[]}]} -> <value>

    val fieldNameRegex= """\w+(?=["'\[])""".r
    val matchResult = fieldNameRegex.findFirstIn(message)
    matchResult.getOrElse("Incorrect field name")
  }

  private def handleValidationRejection(fieldName: String): StandardRoute = {
    val expectedType = getExpectedType(fieldName)
    complete(StatusCodes.BadRequest, getTypeErrorMessage(fieldName, expectedType))
  }

  /**
   * Rejection handler for the Thermometer model
   *
   * If the rejection message contains information about the "id" field,
   * it responds with a BadRequest indicating that "id" is required and provides an example ObjectId
   */
  val thermometerRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case rejection: ValidationRejection if rejection.message.contains("id") =>
          complete(StatusCodes.BadRequest,
            s"id is required. Please provide a valid ObjectId, for example: ${new ObjectId()}")

        case rejection: ValidationRejection =>
          val fieldName = getFieldName(rejection.message)
          handleValidationRejection(fieldName)
      }
      .result()

  /**
   * Rejection handler for the ThermometerEditor model
   *
   * If the rejection message contains information about the "thermometerId" field,
   * it responds with a BadRequest indicating that "thermometerId" is required as a String
   */
  val thermometerEditorRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case rejection: ValidationRejection if rejection.message.contains("thermometerId") =>
          complete(StatusCodes.BadRequest, "thermometerId is required. Please provide a valid String")

        case rejection: ValidationRejection =>
          thermometerRejectionHandler(Seq(rejection)) match {
            case Some(route) => route // Use the provided route
            case None =>
              complete(StatusCodes.InternalServerError, "Internal server error")
          }
      }
      .result()
}
