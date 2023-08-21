package api

import actors.{MongoActor, ThermometerActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import messages.MongoMessages.{CreateReport, DropDatabase, ThermometerEditor}
import org.junit.{Before, Test}
import org.mongodb.scala.bson.{Document, ObjectId}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import simulators.{Thermometer, ThermometerAction}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class RestRoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest with RestRoutes with BeforeAndAfter {
  // Create a test instance of your RestRoutes trait
  private val testActorSystem: ActorSystem = ActorSystem("test-system")

  override def createMongoDbActor(): ActorRef = testActorSystem.actorOf(Props(new MongoActor(databaseName = "test-db")))

  override implicit val requestTimeout: Timeout = 5.seconds

  val testRoutes: Route = route
  private var databaseDropped = false

  private def dropDatabase(): Unit = {
    if (!databaseDropped) {
      mongoActor ! DropDatabase
      databaseDropped = true

    }
  }

  def before[T](before: => Unit)(t: => T): T = {
    before
    try t
  }


  override protected def beforeAll(): Unit = {
    // Drop the test before running the tests to ensure a clean state
    dropDatabase()
  }

  "RestRoutes" should {

    // GET thermometers/list test
    "return a 204 for GET /api/v1/thermometers/list with no content" in {
      // Define a sample GET request
      val request = Get("/api/v1/thermometers/list?page=1&page_size=10")

      // Send the request to the test routes
      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
    // GET thermometers/create test
    "return a 201 for POST api/v1/thermometers/create with thermometer id" in {
      val thermometer = Thermometer(new ObjectId("64dfa3dc8e655049117e49b4"),
        Some("test"), None, None)
      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 201 for POST api/v1/thermometers/create with thermometer without parameters" in {
      val thermometer = Thermometer(None, None, None)
      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 204 for POST api/v1/thermometers/create with duplicated thermometer id" in {
      val thermometer = Thermometer(new ObjectId("64dfa3dc8e655049117e49b4"),
        Some("test"), None, None)
      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    // GET thermometers/list test
    "return a 200 for GET /api/v1/thermometers/list with no content" in {
      // Define a sample GET request
      val request = Get("/api/v1/thermometers/list?page=1&page_size=10")

      // Send the request to the test routes
      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }
    // GET thermometers/{_id: String} test
    "return a 200 for GET api/v1/thermometers/{_id: String} with existing id" in {
      val request = Get("/api/v1/thermometers/64dfa3dc8e655049117e49b4")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        println(response.toString())
      }
    }

    "return a 204 for GET api/v1/thermometers/{_id: String} with not existing id" in {
      val request = Get("/api/v1/thermometers/64dfa3dc8e655049117e49b0")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    // PATCH thermometers/update test
    "return a 200 for PATCH api/v1/thermometers/update/ with existing id" in {
      val thermometerEditor = ThermometerEditor("64dfa3dc8e655049117e49b4",
        Thermometer(new ObjectId("64dfa3dc8e655049117e49b4"),
          Some("updated-test"), None, None))
      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }

    }

    "return a 400 for PATCH api/v1/thermometers/update/ with not existing id" in {
      val thermometerEditor = ThermometerEditor("34dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))
      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for PATCH api/v1/thermometers/update/ with id updating" in {
      val thermometerEditor = ThermometerEditor("64dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))
      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for PATCH api/v1/thermometers/update/ with incorrect id" in {
      val thermometerEditor = ThermometerEditor("34dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))
      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    // DELETE thermometers/delete/{_id: String} test
    "return a 200 for DELETE api/v1/thermometers/delete/{_id: String} with existing&correct id" in {
      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117e49b4")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 400 for DELETE api/v1/thermometers/delete/{_id: String} with incorrect id" in {
      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117e49b")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for DELETE api/v1/thermometers/delete/{_id: String} with not existing id" in {
      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117749b4")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    // GET api/v1/thermometers/reports/list test

    val action = ThermometerAction(new ObjectId("64dfa3dc8e655049117e49b4"), Some(10))
    val thermometerActor = testActorSystem.actorOf(Props(ThermometerActor(requestTimeout, mongoActor)))
    thermometerActor ! action
    "return a 200 for GET api/v1/thermometers/reports/list for correct page" in {
      val request = Get("/api/v1/thermometers/reports/list?page=1&page_size=10")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 204 for GET api/v1/thermometers/reports/list for incorrect page" in {
      val request = Get("/api/v1/thermometers/reports/list?page=2&page_size=10")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

  }
}



