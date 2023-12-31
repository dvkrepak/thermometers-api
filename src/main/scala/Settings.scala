import actors.{MongoActor, ThermometerActor}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import api.RestApi
import simulators.{DataGetter, Thermometer, ThermometerAdapter}

import scala.annotation.unused
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object Settings extends App {
  // Akka settings
  implicit val system: ActorSystem = ActorSystem("MainActorSystem")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Mock data for system start
  private val thermometer = Thermometer.withDefaultCreated(Thermometer(description = Some("Test thermometer"), None, None))
  private val mongoActor = system.actorOf(Props(new MongoActor()), "MongoActor")
  private val actor = system.actorOf(Props(ThermometerActor(5.seconds, mongoActor)), "ThermometerActor")
  private val adapter = new ThermometerAdapter(thermometer, actor)

  // Rest API settings
  private val restApi: RestApi = new RestApi(system, 5.seconds)
  Http().newServerAt("localhost", 8080).bind(restApi.route)

  // Simulate data getter
  private val dataGetter: DataGetter = DataGetter(adapter, system)

  // Data getter settings
  private val isActive: Boolean = false // Set `true` if you want to generate new data
  if (isActive) {
    val delay = 0.seconds // Change to your desired delay
    val interval = 10.seconds // Change to your desired interval
    @unused
    val cancellable = dataGetter.dataRequestScheduler(delay, interval)

    // You can cancel the scheduler when you're done (optional)
    // cancellable.cancel()
  }
}
