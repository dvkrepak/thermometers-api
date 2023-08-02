package main

import akka.actor.{ActorSystem, Props}
import actors.ThermometerActor
import simulators.Thermometer

object Main extends App {
  val system = ActorSystem("Actors-System")
  val thermometerOne = system.actorOf(Props[ThermometerActor](), "ThermometerOne")
  val thermometer = new Thermometer()
  for ( _ <- 1 to 10) thermometerOne ! thermometer.getTemperature

  system.terminate()
}
