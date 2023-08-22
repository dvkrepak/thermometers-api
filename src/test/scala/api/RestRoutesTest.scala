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

  val thermometerActor: ActorRef =
    testActorSystem.actorOf(Props(ThermometerActor(requestTimeout, mongoActor)))

  private def dropDatabase(): Unit = {
    mongoActor ! DropDatabase
  }

  override protected def beforeAll(): Unit = {
    // Drop the database before running the tests to ensure a clean state
    dropDatabase()

    val actionPositiveTen = ThermometerAction(new ObjectId("64dfa3dc8e655049117e49b4"), Some(10))
    Thread.sleep(1)
    val actionNegativeTen = ThermometerAction(new ObjectId("64dfa3dc8e655049117e49b4"), Some(-10))
    Thread.sleep(1)
    val actionNull = ThermometerAction(new ObjectId("64dfa3dc8e655049117e49b4"), None)

    Seq(actionPositiveTen, actionNegativeTen, actionNull).foreach { action =>
      thermometerActor ! action
    }
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

      val correctResponse: String =
        "AcknowledgedInsertOneResult{insertedId=BsonObjectId{value=64dfa3dc8e655049117e49b4}}"

      request ~> route ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }
    }

    "return a 201 for POST api/v1/thermometers/create with thermometer without parameters" in {
      val thermometer = Thermometer(None, None, None)

      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      val correctResponsePart: String =
        "AcknowledgedInsertOneResult{insertedId=BsonObjectId{"

      request ~> route ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString should include(correctResponsePart)
      }
    }

    "return a 204 for POST api/v1/thermometers/create with duplicated thermometer id" in {
      val thermometer = Thermometer(new ObjectId("64dfa3dc8e655049117e49b4"),
        Some("test"), None, None)

      val request = Post("/api/v1/thermometers/create")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometer).toString())

      val correctResponsePart: String =
        "Write error: WriteError{code=11000, message='E11000 duplicate key error collection:"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString should include(correctResponsePart)
      }
    }

    // GET thermometers/list test
    "return a 200 for GET /api/v1/thermometers/list with content" in {

      val request = Get("/api/v1/thermometers/list?page=1&page_size=10")

      val correctResponseParts: Seq[String] = Seq(

        "FulfilledFuture({\"count\":2,\"results\":[{\"_id\":{\"$oid\":\"64dfa3dc8e655049117e49b4\"}," +
          "\"description\":\"test\"",

          "}],\"page\":1,\"page_size\":10,\"next_page\":false})"

      )

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString

        correctResponseParts.foreach { part =>
          responseString should include(part)
        }
      }
    }

    // GET thermometers/{_id: String} test
    "return a 200 for GET api/v1/thermometers/{_id: String} with existing id" in {

      val request = Get("/api/v1/thermometers/64dfa3dc8e655049117e49b4")

      val correctResponsePart =
        "FulfilledFuture([{\"_id\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"description\": \"test\", \"createdAt\": "

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString

        responseString should include(correctResponsePart)
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

      val correctResponse = "AcknowledgedUpdateResult{matchedCount=1, modifiedCount=1, upsertedId=null}"

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }

    }

    "return a 400 for PATCH api/v1/thermometers/update/ with not existing id" in {
      val thermometerEditor = ThermometerEditor("34dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))

      val correctResponse = "AcknowledgedUpdateResult{matchedCount=0, modifiedCount=0, upsertedId=null}"

      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for PATCH api/v1/thermometers/update/ with id updating" in {
      val thermometerEditor = ThermometerEditor("64dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))

      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      val correctResponsePart =
        "Write error: WriteError{code=66, message='Performing an update on the path '_id' " +
          "would modify the immutable field '_id'', details={}}."

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString should include(correctResponsePart)
      }
    }

    "return a 400 for PATCH api/v1/thermometers/update/ with incorrect id" in {
      val thermometerEditor = ThermometerEditor("34dfa3dc8e655049117e49b4",
        Thermometer(Some("updated-test"), None, None))

      val request = Patch("/api/v1/thermometers/update")
        .withEntity(ContentTypes.`application/json`, Json.toJson(thermometerEditor).toString())

      val correctResponse =
        "AcknowledgedUpdateResult{matchedCount=0, modifiedCount=0, upsertedId=null}"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }
    }

    // DELETE thermometers/delete/{_id: String} test
    "return a 200 for DELETE api/v1/thermometers/delete/{_id: String} with existing&correct id" in {

      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117e49b4")

      val correctRespond = "AcknowledgedDeleteResult{deletedCount=1}"

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctRespond
      }
    }

    "return a 400 for DELETE api/v1/thermometers/delete/{_id: String} with incorrect id" in {

      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117e49b")

      val correctResponse = "Error: id must have exactly 24 letters and consist of hex letters and digits only"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for DELETE api/v1/thermometers/delete/{_id: String} with not existing id" in {

      val request = Delete("/api/v1/thermometers/delete/64dfa3dc8e655049117749b4")

      val correctResponse = "AcknowledgedDeleteResult{deletedCount=0}"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }
    }

    // GET api/v1/thermometers/reports/list test
    "return a 200 for GET api/v1/thermometers/reports/list for correct page" in {

      val request = Get("/api/v1/thermometers/reports/list?page=1&page_size=10")

      val correctResponseParts: Seq[String] = Seq(

        "FulfilledFuture({\"count\":1,\"results\":[{\"lastTemperature\":null,",

        "\"thermometerId\":{\"$oid\":\"64dfa3dc8e655049117e49b4\"}}],\"page\":1,\"page_size\":10,\"next_page\":false})"

      )

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        correctResponseParts.foreach{
          part => responseString should include(part)
        }
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

      val correctResponseParts: Seq[String] = Seq(

        "FulfilledFuture({\"count\":1,\"results\":[{\"lastTemperature\":null,",

        "\"thermometerId\":{\"$oid\":\"64dfa3dc8e655049117e49b4\"}}],\"page\":1,\"page_size\":10,\"next_page\":false})"

      )

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        correctResponseParts.foreach {
          part => responseString should include(part)
        }
      }
    }

    // GET api/v1/thermometers/reports/{_id: String}/?from={Date}&till={Date} test
    "return a 200 for GET api/v1/thermometers/reports/{_id} with valid parameters" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      val correctResponseParts: Seq[String] = Seq(

        "\"thermometerId\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"temperature\": 10",
        "\"thermometerId\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"temperature\": -10",
        "\"thermometerId\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"temperature\": null"

      )

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        correctResponseParts.foreach {
          part => responseString should include(part)
        }
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

      val correctResponse =
        "FulfilledFuture(Error: createdAtMin and createdAtMax must be in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSZ')"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for GET api/v1/thermometers/reports/{_id} with incorrect createdAtMin" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "start-world"
      val createdAtMax = "2000-01-31T23:59:59.999-0000"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      val correctResponse =
        "FulfilledFuture(Error: createdAtMin and createdAtMax must be in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSZ')"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for GET api/v1/thermometers/reports/{_id} with incorrect createdAtMax" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b4"
      val createdAtMin = "2000-01-31T23:59:59.999-0000"
      val createdAtMax = "start-world"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      val correctResponse =
        "FulfilledFuture(Error: createdAtMin and createdAtMax must be in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSZ')"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for GET api/v1/thermometers/reports/{_id} with incorrect id" in {

      // Define sample parameters
      val thermometerId = "64dfa3dc8e655049117e49b" // Short id
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"

      val request = Get(s"/api/v1/thermometers/reports/$thermometerId?from=$createdAtMin&till=$createdAtMax")

      val correctResponse =
        "FulfilledFuture(Error: thermometerId must have exactly 24 letters and consist of hex letters and digits only)"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
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

      val correctResponse =
        "FulfilledFuture([{\"thermometerId\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"minTemperature\": -10}])"

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 200 for GET api/v1/thermometers/statistics with valid parameters and maximum operation" in {

      // Define valid parameters for the maximum operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"
      val maximumOpt = true

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax&tmp_max=$maximumOpt")

      val correctResponse =
        "FulfilledFuture([{\"thermometerId\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"maxTemperature\": 10}])"

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 200 for GET api/v1/thermometers/statistics with valid parameters and average operation" in {

      // Define valid parameters for the average operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"
      val averageOpt = true

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax&tmp_avg=$averageOpt")

      val correctResponse =
        "FulfilledFuture([{\"thermometerId\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"avgTemperature\": 0.0}])"

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 200 for GET api/v1/thermometers/statistics with valid parameters and median operation" in {

      // Define valid parameters for the median operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"
      val medianOpt = true

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax&tmp_med=$medianOpt")

      val correctResponse =
        "FulfilledFuture([{\"thermometerId\": {\"$oid\": \"64dfa3dc8e655049117e49b4\"}, \"median\": -10}])"

      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
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

      val correctResponse =
        "Exactly one operation from minimum/maximum/average/median must be selected."

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for GET api/v1/thermometers/statistics with invalid createdAtMax parameter" in {

      // Define invalid createdAtMax parameters for the minimum operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "hi"
      val minimumOpt = true

      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax" +
        s"&tmp_min=$minimumOpt")

      val correctResponse =
        "FulfilledFuture(Error: createdAtMin and createdAtMax must be in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSZ')"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for GET api/v1/thermometers/statistics without one date parameter" in {

      // Define valid parameters for the minimum operation without createdAtMax
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val minimumOpt = true

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=" +
        s"&tmp_min=$minimumOpt")

      val correctResponse =
        "FulfilledFuture(Error: createdAtMin and createdAtMax must be in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSZ')"

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseEntity = response.entity
        val responseString = responseEntity.toStrict(5.seconds).map(_.data.utf8String).toString
        responseString shouldBe correctResponse
      }
    }

    "return a 400 for GET api/v1/thermometers/statistics without operation" in {

      // Define valid parameters without operation
      val createdAtMin = "2000-01-01T00:00:00.000-0000"
      val createdAtMax = "2050-01-31T23:59:59.999-0000"

      // Send the GET request to the test route
      val request = Get(s"/api/v1/thermometers/statistics?from=$createdAtMin&till=$createdAtMax")

      val correctResponse =
        "Exactly one operation from minimum/maximum/average/median must be selected."

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        contentType shouldBe ContentTypes.`application/json`

        val responseString = responseAs[String]
        responseString shouldBe correctResponse
      }
    }
  }
}
