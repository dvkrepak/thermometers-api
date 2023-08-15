package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import messages.MongoMessages._
import org.mongodb.scala.bson.{BsonObjectId, Document}
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.Future

trait ThermometerApi {

  def createMongoDbActor(): ActorRef

  implicit val requestTimeout: Timeout
  private lazy val mongoActor: ActorRef = createMongoDbActor()

  private def validateId(id: String, name: String): Unit = {
    if (!Validators.requireCorrectId(id)) {
      throw new IllegalArgumentException(
        s"$name must have exactly 24 letters and consist of hex letters and digits only")
    }
  }

  protected def getThermometers: Future[Seq[Document]] = {
    (mongoActor ? FindAllThermometers).mapTo[Seq[Document]]
  }

  protected def createThermometer(jsonString: String): Future[BsonObjectId] = {
    (mongoActor ? CreateThermometer(jsonString)).mapTo[BsonObjectId]
  }

  protected def editThermometer(thermometerId: String, json: String): Future[UpdateResult] = {
    validateId(thermometerId, "thermometerId")

    (mongoActor ? UpdateThermometer(thermometerId, json)).mapTo[UpdateResult]
  }

  protected def findThermometer(_id: String): Future[Option[Document]] = {
    validateId(_id, "_id")

    (mongoActor ? FindThermometer(_id)).mapTo[Option[Document]]
  }

  protected def deleteThermometer(_id: String): Future[Long] = {
    validateId(_id, "_id")

    (mongoActor ? DeleteThermometer(_id)).mapTo[Long]
  }

  protected def createData(json: String): Future[BsonObjectId] = {
    (mongoActor ? CreateData(json)).mapTo[BsonObjectId]
  }

  protected def findDataWithRangeWithId(thermometerId: String,
                                        createdAtMin: String,
                                        createdAtMax: String): Future[Seq[Document]] = {
    validateId(thermometerId, "thermometerId")

    val correctDates = Validators.requireCorrectDateFormat(createdAtMax) &&
      Validators.requireCorrectDateFormat(createdAtMax)

    if (!correctDates) {
       throw new IllegalArgumentException(
         "createdAtMin and createdAtMax must be in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSZ'")
    }

    (mongoActor ? FindDataWithRangeWithId(thermometerId, createdAtMin, createdAtMax)).mapTo[Seq[Document]]
  }

  protected def findDataWithId(thermometerId: String): Future[Seq[Document]] = {
    validateId(thermometerId, "thermometerId")

    (mongoActor ? FindDataWithId(thermometerId)).mapTo[Seq[Document]]
  }

  protected def findDataSummarized(): Future[Seq[Document]] = {
    (mongoActor ? FindDataSummarized).mapTo[Seq[Document]]
  }
}
