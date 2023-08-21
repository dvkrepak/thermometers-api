package api

import actors.{MongoActor, ThermometerActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import messages.MongoMessages.{DropDatabase, ThermometerEditor}
import org.mongodb.scala.bson.ObjectId
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import simulators.{Thermometer, ThermometerAction}

import scala.concurrent.duration.DurationInt

class RestRoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest with RestRoutes with BeforeAndAfter {

  private val testActorSystem: ActorSystem = ActorSystem("test-system")

  override def createMongoDbActor(): ActorRef = testActorSystem.actorOf(Props(new MongoActor(databaseName = "test-db")))

  override implicit val requestTimeout: Timeout = 5.seconds

  private def dropDatabase(): Unit = {
    mongoActor ! DropDatabase
  }

  override protected def beforeAll(): Unit = {
    // Drop the database before running the tests to ensure a clean state
    dropDatabase()
  }

  override protected def afterAll(): Unit = {
    // Drop the database after running the tests to ensure a clean state
    dropDatabase()
  }

  "RestRoutes" should {

    // GET thermometers/list test
    "return a 204 for GET /api/v1/thermometers/list with no content" in {

      val request = Get("/api/v1/thermometers/list?page=1&page_size=10")

      request ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    // GET thermometers/create test
    "return a 201 for POST api/v1/thermometers/create with thermometer id" in {
      val thermometer = Thermometer(new ObjectId("64dfa3dc8e655049117e49b4"),
        Some("test"), None, None)

      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      request ~> route ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 201 for POST api/v1/thermometers/create with thermometer without parameters" in {
      val thermometer = Thermometer(None, None, None)

      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      request ~> route ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 204 for POST api/v1/thermometers/create with duplicated thermometer id" in {
      val thermometer = Thermometer(new ObjectId("64dfa3dc8e655049117e49b4"),
        Some("test"), None, None)

      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    // GET thermometers/list test
    "return a 200 for GET /api/v1/thermometers/list with content" in {

      val request = Get("/api/v1/thermometers/list?page=1&page_size=10")


      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    // GET thermometers/{_id: String} test
    "return a 200 for GET api/v1/thermometers/{_id: String} with existing id" in {
      val request = Get("/api/v1/thermometers/64dfa3dc8e655049117e49b4")

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 204 for GET api/v1/thermometers/{_id: String} with not existing id" in {
      val request = Get("/api/v1/thermometers/64dfa3dc8e655049117e49b0")

      request ~> route ~> check {
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

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }

    }

    "return a 400 for PATCH api/v1/thermometers/update/ with not existing id" in {
      val thermometerEditor = ThermometerEditor("34dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))

      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for PATCH api/v1/thermometers/update/ with id updating" in {
      val thermometerEditor = ThermometerEditor("64dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))

      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for PATCH api/v1/thermometers/update/ with incorrect id" in {
      val thermometerEditor = ThermometerEditor("34dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))

      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    // DELETE thermometers/delete/{_id: String} test
    "return a 200 for DELETE api/v1/thermometers/delete/{_id: String} with existing&correct id" in {

      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117e49b4")

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 400 for DELETE api/v1/thermometers/delete/{_id: String} with incorrect id" in {

      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117e49b")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for DELETE api/v1/thermometers/delete/{_id: String} with not existing id" in {

      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117749b4")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }


    // START
    // Add thermometer report to a database
    val action = ThermometerAction(new ObjectId("64dfa3dc8e655049117e49b4"), Some(10))
    val thermometerActor = testActorSystem.actorOf(Props(ThermometerActor(requestTimeout, mongoActor)))
    thermometerActor ! action
    // END

    // GET api/v1/thermometers/reports/list test
    "return a 200 for GET api/v1/thermometers/reports/list for correct page" in {

      val request = Get("/api/v1/thermometers/reports/list?page=1&page_size=10")

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 204 for GET api/v1/thermometers/reports/list for incorrect page" in {

      val request = Get("/api/v1/thermometers/reports/list?page=2&page_size=10")

      request ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "return a 200 for GET api/v1/thermometers/reports/list without pagination" in {

      val request = Get("/api/v1/thermometers/reports/list")

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    // GET api/v1/thermometers/reports/{_id: String}/?from={Date}&till={Date} test
    "return a 200 for GET api/v1/thermometers/reports/{_id} with valid parameters" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 204 for GET api/v1/thermometers/reports/{_id} with impossible date parameters" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "2010-01-01T00:00:00.000-0000"
      val createdAtMax = "2000-01-31T23:59:59.999-0000"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      request ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "return a 400 for GET api/v1/thermometers/reports/{_id} with incorrect date" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "start-world"
      val createdAtMax = "end-world"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for GET api/v1/thermometers/reports/{_id} with incorrect createdAtMin" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "start-world"
      val createdAtMax = "2000-01-31T23:59:59.999-0000"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for GET api/v1/thermometers/reports/{_id} with incorrect createdAtMax" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "2000-01-31T23:59:59.999-0000"
      val createdAtMax = "start-world"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return a 400 for GET api/v1/thermometers/reports/{_id} with incorrect id" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b" // Short id
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    // GET api/v1/thermometers/statistics/?from={Date}&till={Date}&tmp_min={Boolean} test
    "return a 200 for GET api/v1/thermometers/statistics with valid parameters and minimum operation" in {

      // Define valid parameters for the minimum operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"
      val minimumOpt = true

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax&tmp_min=$minimumOpt")

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 400 for GET api/v1/thermometers/statistics with valid parameters and two operations" in {

      // Define parameters for the minimum&average operations
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"
      val minimumOpt = true
      val averageOpt = true

      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax" +
        s"&tmp_min=$minimumOpt&tmp_avg=$averageOpt")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 400 for GET api/v1/thermometers/statistics with invalid date parameters" in {

      // Define invalid parameters for the minimum operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "hi"
      val minimumOpt = true

      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax" +
        s"&tmp_min=$minimumOpt")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 400 for GET api/v1/thermometers/statistics without one date parameter" in {

      // Define valid parameters for the minimum operation without createdAtMax
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val minimumOpt = true

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=" +
        s"&tmp_min=$minimumOpt")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }

    "return a 400 for GET api/v1/thermometers/statistics without operation" in {

      // Define valid parameters without operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax")

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`
      }
    }
  }
}
