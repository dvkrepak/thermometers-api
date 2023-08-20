package utils

import org.mongodb.scala.bson.Document

import scala.concurrent.Future


object RouteUtils {
  sealed trait Operation
  object Minimum extends Operation
  object Maximum extends Operation
  object Average extends Operation
  object Median extends Operation
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

  /**
   * Builds a resolver for operations, mapping each operation to a corresponding function.
   *
   * @param functions A sequence of functions that take two strings as input parameters and return
   *                  a future containing a sequence of documents.
   * @return A map that associates each supported operation with its corresponding function.
   * @note This function assumes that the 'operations' variable is defined as a sequence of supported
   *       operations, such as Seq(Minimum, Maximum, Average, Median). The order of operations in
   *       the 'functions' parameter should align with the order of operations in 'operations'.
   */
  def buildOperationResolver(functions: Seq[(String, String) =>
      Future[Seq[Document]]]): Map[Operation, (String, String) => Future[Seq[Document]]] = {
    operations.zip(functions).toMap
  }

  def getStatisticFunction(operation: Operation,
                           resolver: Map[Operation, (String, String) =>
                             Future[Seq[Document]]]): Option[(String, String) => Future[Seq[Document]]] = {
    resolver.get(operation)
  }
}
