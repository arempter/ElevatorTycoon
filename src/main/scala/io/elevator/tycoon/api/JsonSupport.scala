package io.elevator.tycoon.api

import io.elevator.tycoon.domain.ElevatorStatus
import spray.json.DefaultJsonProtocol._

trait JsonSupport {
  case class ElevatorsStatus(elevators: Seq[ElevatorStatus])

  implicit val elevatorStatusF = jsonFormat3(ElevatorStatus)
  implicit val elevatorsStatusF = jsonFormat1(ElevatorsStatus)

}
