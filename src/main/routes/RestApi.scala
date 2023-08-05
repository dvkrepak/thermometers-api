package routes

import akka.actor.ActorSystem
import akka.util.Timeout

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes {
}
