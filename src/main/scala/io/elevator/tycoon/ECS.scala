package io.elevator.tycoon

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.elevator.tycoon.api.ElevatorControlUserAPI

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

trait ECS extends LazyLogging with ElevatorControlUserAPI {
  protected implicit def system: ActorSystem
  protected implicit val ec: ExecutionContext

  // The routes we serve.
  final val allRoutes = pathPrefix("api") { routes }

  lazy val startup: Future[Http.ServerBinding] =
    // should be moved to application.conf
    // listen on 0.0.0.0 helps with docker if used
    Http(system).bindAndHandle(allRoutes, "0.0.0.0", 8080)
      .andThen {
        case Success(binding) => logger.info(s"ElevatorControlSystem started listening: ${binding.localAddress}")
        case Failure(reason)  => logger.error("ElevatorControlSystem failed to start.", reason)
      }

  def shutdown(): Future[Done] = {
    startup.flatMap(_.unbind)
      .andThen {
        case Success(_)      => logger.info("ElevatorControlSystem stopped.")
        case Failure(reason) => logger.error("ElevatorControlSystem failed to stop.", reason)
      }
  }

}
