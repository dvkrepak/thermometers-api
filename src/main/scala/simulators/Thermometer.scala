package simulators

import java.util.Date
import scala.util.Random

case class Thermometer(description: Option[String],
                       created_at: Option[Date],
                       edited_at: Option[Date]) {

  def getTemperature: Option[Int] = {
    simulateWork()
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
  /**
   * Creates a new 'Thermometer' instance with a default 'created_at' value if empty
   *
   * @param thermometer The original 'Thermometer' instance to be checked.
   * @return Copied from input with `created_at` set to the current date if it was missing,
   *         Instance from input otherwise
   */
  def withDefaultCreated(thermometer: Thermometer): Thermometer = thermometer.created_at match {
    case Some(_) => thermometer
    case None => thermometer.copy(created_at = Some(new Date()))
  }
}