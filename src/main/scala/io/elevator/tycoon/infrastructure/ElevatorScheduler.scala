package io.elevator.tycoon.infrastructure

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import io.elevator.tycoon.domain.ElevatorControlSystem._
import io.elevator.tycoon.domain.{ ElevatorStatus, PickupRequest }

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }

trait ElevatorScheduler extends LazyLogging {
  protected implicit val system: ActorSystem
  protected implicit val ec: ExecutionContext
  protected implicit val mat: Materializer

  def status(): Seq[ElevatorStatus]

  protected def update(newStatus: ElevatorStatus): Unit

  protected def step(): Unit

  private def findElevatorBy(id: Int, floor: Int): Option[ElevatorStatus] = status().find(e => e.id == id && e.floor == floor)
  private def findElevatorBy(id: Int): Option[ElevatorStatus] = status().find(e => e.id == id)

  // try to find stopped or running near
  // if no stopped, return one with goalFloor close to pickup request
  private def findClosestStoppedOrMoving(requestFloor: Int) =
    status()
      .filter(e => e.floor == e.goalFloor) match {
        case s if s.isEmpty => status().minBy(e => math.abs(e.goalFloor - requestFloor))
        case s              => s.minBy(e => math.abs(e.floor - requestFloor))
      }

  // add timer?
  @tailrec
  private def waitForNextArrival(id: Int): Future[ElevatorStatus] =
    findElevatorBy(id) match {
      case Some(e) if e.floor == e.goalFloor => Future.successful(e)
      case Some(e)                           => waitForNextArrival(e.id)
    }

  // todo: add direction in scheduling
  private def findFreeElevatorWithRetry(requestFloor: Int, retry: AtomicLong = new AtomicLong(0), maxTries: Int = 3): Future[ElevatorStatus] = {
    findClosestStoppedOrMoving(requestFloor) match {
      case nextE if nextE.floor == nextE.goalFloor => Future.successful(nextE)
      case nextE =>
        logger.info("All elevators are moving, retrying... for {}", nextE)
        waitForNextArrival(nextE.id)
    }
  }

  def scheduleRide(pickupRequest: PickupRequest): Future[ElevatorStatus] =
    findFreeElevatorWithRetry(pickupRequest.fromFloor).flatMap {
      nextE =>
        scheduleRide(nextE.copy(goalFloor = pickupRequest.fromFloor))
    }

  // todo: accept seq of floors to stop by
  def scheduleRide(newStatus: ElevatorStatus): Future[ElevatorStatus] =
    if (newStatus.goalFloor <= noOfFloors) {
      logger.info("Elevator id {}, going to selected, new floor {}", newStatus.id, newStatus.goalFloor)
      findElevatorBy(newStatus.id, newStatus.floor).map {
        e =>
          val direction = if (newStatus.goalFloor > e.floor) 1 else -1
          dispatchElevator(e.copy(goalFloor = newStatus.goalFloor), direction).map {
            e =>
              logger.info("Elevator id {} arrived at {}, **** user enters/leaves", e.id, e.goalFloor)
              e
          }
      }.getOrElse(
        Future.failed(new Exception(s"Elevator id ${newStatus.id} not on current floor ${newStatus.floor}")))
    } else {
      Future.failed(new Exception("Floor out of building boundary, serious?"))
    }

  // this could be separate trait or class ElevatorEngine etc.
  private def dispatchElevator(newStatus: ElevatorStatus, direction: Int): Future[ElevatorStatus] =
    newStatus match {
      case ElevatorStatus(id, floor, goalFloor) if floor == goalFloor =>
        logger.debug("No need to move elevator id {}, already at destination, **** door opens", id)
        Future.successful(newStatus)
      case ElevatorStatus(id, floor, goalFloor) if direction == -1 =>
        logger.debug("Elevator id {} going down", id)
        for (f <- floor - 1 to goalFloor by -1) {
          logger.info("Elevator id: {}, moving floor {}", id, f)
          update(newStatus.copy(floor = f))
          step()
        }
        Future.successful(newStatus)
      case ElevatorStatus(id, floor, goalFloor) if direction == 1 =>
        logger.debug("Elevator id {} going up", id)
        for (f <- floor + 1 to goalFloor) {
          logger.info("Elevator id: {}, moving floor {}", id, f)
          update(newStatus.copy(floor = f))
          step()
        }
        Future.successful(newStatus)
    }

}

