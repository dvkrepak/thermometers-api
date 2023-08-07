package api

import actors.MongoActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes {
  override implicit val requestTimeout: Timeout = timeout

  override def createMongoDbActor(): ActorRef = system.actorOf(
    Props(new MongoActor()), "MongoActor")
}
