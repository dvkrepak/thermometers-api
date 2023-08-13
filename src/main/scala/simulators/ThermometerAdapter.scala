package simulators

import akka.actor.ActorRef

class ThermometerAdapter(thermometer: Thermometer, actor: ActorRef) {

  def requestData(): Unit = {
    thermometer.requestTemperature(actor)
  }
}
