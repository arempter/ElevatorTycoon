package io.elevator.tycoon.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.elevator.tycoon.domain.{ ElevatorStatus, PickupRequest }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait ElevatorControlUserAPI extends JsonSupport {

  protected def pickup(pickupRequest: PickupRequest): Future[ElevatorStatus]
  protected def ride(newStatus: ElevatorStatus): Future[ElevatorStatus]
  protected def status(): Seq[ElevatorStatus]

  private def execRequest(f: => Future[ElevatorStatus]): Route =
    onComplete(f) {
      case Success(elevatorStatus) => complete(elevatorStatus)
      case Failure(exception)      => complete("Elevator error " + exception.getMessage)
    }

  val routes: Route =
    get {
      path("elevators") {
        complete(ElevatorsStatus(status()))
      }
    } ~ get {
      path("elevators" / "pickup" / IntNumber / Segment) { (floor, direction) =>
        validate(direction.equals("up") || direction.equals("down"), "Direction can be only up or down") {
          val pickupRequest = if (direction == "up") PickupRequest(floor, 1) else PickupRequest(floor, -1)
          execRequest(pickup(pickupRequest))
        }
      }
    } ~ get {
      path("elevators" / IntNumber / "from" / IntNumber / "go" / IntNumber) { (id, floor, goalFloor) =>
        execRequest(ride(ElevatorStatus(id, floor, goalFloor)))
      }
    }

}
