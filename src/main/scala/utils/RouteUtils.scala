package utils

import org.mongodb.scala.bson.Document

import scala.concurrent.Future

object RouteUtils {
  sealed trait Operation
  object Minimum extends Operation
  object Maximum extends Operation
  object Average extends Operation
  object Median extends Operation

  // Define supported operations as a sequence
  private val supportedOperations: Seq[Operation] = Seq(Minimum, Maximum, Average, Median)

  /**
   * Convert options representing selected operations into a single Operation
   *
   * @param minimum Option for Minimum operation
   * @param maximum Option for Maximum operation
   * @param average Option for Average operation
   * @param median  Option for Median operation
   * @return `Some(Operation)` if exactly one operation is selected,
   *         `None` otherwise
   */
  def convertToOperation(minimum: Option[Boolean],
                         maximum: Option[Boolean],
                         average: Option[Boolean],
                         median: Option[Boolean]): Option[Operation] = {
    val selectedOperations = Seq(minimum, maximum, average, median)
    val selectedCount = selectedOperations.count(_.contains(true))

    if (selectedCount == 1) {
      supportedOperations.zip(selectedOperations).collectFirst {
        case (operation, Some(true)) => operation
      }
    } else {
      None
    }
  }

  /**
   * Build an operation resolver that maps operations to corresponding functions
   *
   * @param functions A sequence of functions taking two strings and returning a Future of documents
   * @return A map associating each supported operation with its function
   * @note This function assumes that the `supportedOperations` variable is defined as a sequence of supported
   *       operations, such as Seq(Minimum, Maximum, Average, Median). The order of operations in
   *       the `functions` parameter should align with the order of operations in `operations`
   */
  def buildOperationResolver(functions: Seq[(String, String) =>
      Future[Seq[Document]]]): Map[Operation, (String, String) => Future[Seq[Document]]] = {
    supportedOperations.zip(functions).toMap
  }


  /**
   * Get the function corresponding to a given operation from the resolver
   *
   * @param operation The desired operation
   * @param resolver  A map associating operations with functions
   * @return `Some(function)` if the operation is supported,
   *         `None` otherwise
   */
  def getStatisticFunction(operation: Operation,
                           resolver: Map[Operation, (String, String) =>
                             Future[Seq[Document]]]): Option[(String, String) => Future[Seq[Document]]] = {
    resolver.get(operation)
  }
}
