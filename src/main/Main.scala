import akka.actor.{ActorSystem, Props}
import actors.ThermometerActor
import simulators.Thermometer

object Main extends App {
  private val system = ActorSystem("Actors-System")
  private val thermometerOne = system.actorOf(Props[ThermometerActor](), "ThermometerOne")
  private val thermometer = new Thermometer()
  for ( _ <- 1 to 10) thermometerOne ! thermometer.getTemperature

  system.terminate()
}
