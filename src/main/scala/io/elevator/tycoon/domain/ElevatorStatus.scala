package io.elevator.tycoon.domain

sealed trait RideStatus
case class ElevatorStatus(id: Int, floor: Int = 0, goalFloor: Seq[Int] = Seq(0)) extends RideStatus
case class PickupRequest(fromFloor: Int, direction: Int) extends RideStatus

