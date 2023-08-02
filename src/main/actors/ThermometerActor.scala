package main.actors

import akka.actor.Actor

class ThermometerActor extends Actor {

  override def receive: Receive = {
    case Some(value) => println(s"Received value $value")
    case _ => println("RECEIVED ERROR")
  }
}
