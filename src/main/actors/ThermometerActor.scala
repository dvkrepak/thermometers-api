package actors

import akka.actor.Actor

class ThermometerActor extends Actor {

  override def receive: Receive = {
    case Some(value) => println(s"Received value $value")
    case err => println(s"Received error '$err'")
  }
}
