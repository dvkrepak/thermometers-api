package utils

import org.mongodb.scala.bson.Document

import scala.concurrent.Future


object RouteUtils {
  sealed trait Operation
  private object Minimum extends Operation
  private object Maximum extends Operation
  private object Average extends Operation
  private object Median extends Operation
  private val operations: Seq[Operation] = Seq(Minimum, Maximum, Average, Median)


  def convertToOperation(minimum: Option[Boolean],
                         maximum: Option[Boolean],
                         average: Option[Boolean],
                         median: Option[Boolean]): Option[Operation] = {
    val selectedOperations = Seq(minimum, maximum, average, median)
    val selectedCount = selectedOperations.count(_.contains(true))

    if (selectedCount == 1) {
      operations.zip(selectedOperations).collectFirst {
        case (operation, Some(true)) => operation
      }
    } else {
      None
    }

  }

  def buildOperationResolver(functions: Seq[(String, String) =>
      Future[Seq[Document]]]): Map[Operation, (String, String) => Future[Seq[Document]]] = {
    operations.zip(functions).toMap
  }

  def getStatisticFunction(operation: Operation,
                           resolver: Map[Operation, (String, String) =>
                             Future[Seq[Document]]]): (String, String) => Future[Seq[Document]] = {
    resolver(operation)
  }
}
