package simulators

import akka.actor.ActorRef
import org.mongodb.scala.bson.ObjectId


import java.util.Date
import scala.util.Random

case class Thermometer(_id: ObjectId = new ObjectId(),
                       description: Option[String] = None,
                       createdAt: Option[Date] = None,
                       editedAt: Option[Date] = None) {

  require(_id.toString.length == 24)

  def requestTemperature(actor: ActorRef): Unit = {
    actor ! ThermometerAction(thermometerId = _id, simulateWork())
  }

  /**
   * Simulates the temperature measurement of the Thermometer
   * Produces correct temperature with 80% rate
   *
   * @return `Some(Int)` if operation is successful, where `Int` is in range [-100, 100]
   *         `None` otherwise
   */
  private def simulateWork(): Option[Int] = {
    if (math.random < 0.80) {
      Some(new Random().between(-100, 100))
    } else {
      None
    }
  }
}


object Thermometer {

  def apply(description: Option[String], createdAt: Option[Date], editedAt: Option[Date]): Thermometer = {
    val _id = new ObjectId()
    Thermometer(_id, description, createdAt, editedAt)
  }

  /**
   * Creates a new 'Thermometer' instance with a default 'created_at' value if empty
   *
   * @param thermometer The original 'Thermometer' instance to be checked.
   * @return Copied from input with `created_at` set to the current date if it was missing,
   *         Instance from input otherwise
   */
  def withDefaultCreated(thermometer: Thermometer): Thermometer = thermometer.createdAt match {
    case Some(_) => thermometer
    case None => thermometer.copy(createdAt = Some(new Date()))
  }

  /**
   * Creates new `Thermometer object` with 'edited_at' field set to current date and time
   *
   * @param thermometer The `Thermometer` object to update the 'edited_at' field for
   * @return A new `Thermometer` object with the `edited_at` field set to the current date and time.
   */
  def setEditedAt(thermometer: Thermometer): Thermometer = {
    thermometer.copy(editedAt = Some(new Date()))
  }
}