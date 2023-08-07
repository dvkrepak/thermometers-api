import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http

import actors.MongoActor
import scala.concurrent.ExecutionContextExecutor

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("Actors-System")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private val mongoDbActor = system.actorOf(Props(new MongoActor("mongodb://localhost:27017", "optimsys-db")), "MongoActor")


  import routes.RestRoutes
  private val restRoutes = new RestRoutes(mongoDbActor)


  Http().newServerAt("localhost", 8080).bind(restRoutes.route)
}
