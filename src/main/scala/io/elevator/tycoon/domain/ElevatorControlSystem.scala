package io.elevator.tycoon.domain

import scala.concurrent.Future

object ElevatorControlSystem {
  // should be application.conf
  val noOfFloors = 10
  val noOfElevators = 16

}

trait ElevatorControlSystem {

  def status(): Seq[ElevatorStatus]
  def update(e: ElevatorStatus): Unit // status update after move - send by Elevator after every floor
  def pickup(p: PickupRequest): Future[ElevatorStatus] // A pickup request is two integers: Pickup Floor, Direction (negative for down, positive for up)
  def ride(e: ElevatorStatus): Future[ElevatorStatus] // added this one to support case where user enters the elevator and selects new ride
  def step(): Unit // time step, moving floor

}
