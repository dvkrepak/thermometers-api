package simulators

import scala.util.Random

class Thermometer {

  def getTemperature: Option[Int] = {
    simulateWork()
  }

  /**
   * Simulates the temperature measurement of the Thermometer
   * Produces correct temperature with 80% rate
   *
   * @return 'Some(Int)' if operation is successful, where 'Int' is in range [-100, 100]
   *         'None' otherwise
   */
  private def simulateWork(): Option[Int] = {
    // Generates correct value with 80% of success
    if (math.random < 0.80) {
      Some(new Random().between(-100, 100))
    } else {
      None
    }
  }
}
