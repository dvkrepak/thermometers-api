import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import api.RestApi

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt


object Main extends App {
  implicit val system: ActorSystem = ActorSystem("Actors-System")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private val restApi: RestApi = new RestApi(system, 5.seconds)


  Http().newServerAt("localhost", 8080).bind(restApi.route)
}
