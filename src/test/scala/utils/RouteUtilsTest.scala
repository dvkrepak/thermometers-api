package utils

import actors.MongoActor
import org.mongodb.scala.bson.Document
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class RouteUtilsTest extends AnyWordSpec with Matchers {

  "convertToOperation" should {

    "return Some(Minimum) when minimum is true and other options are false" in {
      // Arrange
      val minimum = Some(true)
      val maximum = Some(false)
      val average = Some(false)
      val median = Some(false)

      // Act
      val result = RouteUtils.convertToOperation(minimum, maximum, average, median)

      // Assert
      result shouldBe Some(RouteUtils.Minimum)
    }

    "return Some(Average) when average is true and other options are false" in {
      // Arrange
      val minimum = Some(false)
      val maximum = Some(false)
      val average = Some(true)
      val median = Some(false)

      // Act
      val result = RouteUtils.convertToOperation(minimum, maximum, average, median)

      // Assert
      result shouldBe Some(RouteUtils.Average)
    }

    "return None when average and maximum are both true" in {
      // Arrange
      val minimum = Some(false)
      val maximum = Some(true)
      val average = Some(true)
      val median = Some(false)

      // Act
      val result = RouteUtils.convertToOperation(minimum, maximum, average, median)

      // Assert
      result shouldBe empty
    }

    "return None when all options are false" in {
      // Arrange
      val minimum = Some(false)
      val maximum = Some(false)
      val average = Some(false)
      val median = Some(false)

      // Act
      val result = RouteUtils.convertToOperation(minimum, maximum, average, median)

      // Assert
      result shouldBe empty
    }


  }

  "getStatisticFunction" should {

    "return the minimum function when minimum function is provided in the resolver" in {
      // Arrange
      val minimumFunction = (createdAtMin: String, createdAtMax: String) => {
        val actor = new MongoActor()
        actor.getCollection("collection")
        MongoUtils.findMinimumDataWithRange(actor.getCollection("collection"), createdAtMin, createdAtMax)
      }
      val resolver = Map(RouteUtils.Minimum.asInstanceOf[RouteUtils.Operation] -> minimumFunction)

      // Act
      val result = RouteUtils.getStatisticFunction(RouteUtils.Minimum, resolver)

      // Assert
      result shouldBe Some(minimumFunction)
    }

    "return None when minimum function is not provided in the resolver" in {
      // Arrange
      val resolver: Map[RouteUtils.Operation, (String, String) => Future[Seq[Document]]] = Map()

      // Act
      val result = RouteUtils.getStatisticFunction(RouteUtils.Minimum, resolver)

      // Assert
      result shouldBe empty
    }
  }
}
