package simulators

import akka.actor.{ActorSystem, Cancellable}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}

case class DataGetter(private val adapter: ThermometerAdapter, private val system: ActorSystem)
                  (implicit val executionContext: ExecutionContextExecutor) {

  private def performDataRequest(): Future[Unit] = Future {
    adapter.requestData()
  }

  def dataRequestScheduler(initialDelay: FiniteDuration, interval: FiniteDuration): Cancellable = {
    val cancellable = system.scheduler.scheduleWithFixedDelay(initialDelay, interval) {
      () =>
        performDataRequest()
    }
    cancellable
  }
}
