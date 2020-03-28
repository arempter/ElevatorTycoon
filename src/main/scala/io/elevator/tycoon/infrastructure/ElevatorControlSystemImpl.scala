package io.elevator.tycoon.infrastructure

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.{ Materializer, OverflowStrategy, QueueOfferResult }
import com.typesafe.scalalogging.LazyLogging
import io.elevator.tycoon.domain.ElevatorControlSystem._
import io.elevator.tycoon.domain.{ ElevatorControlSystem, ElevatorStatus, PickupRequest }

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

trait ElevatorControlSystemImpl extends ElevatorControlSystem with LazyLogging with ElevatorScheduler {
  protected implicit val system: ActorSystem
  protected implicit val ec: ExecutionContext
  protected implicit val mat: Materializer

  val nextFloorTime: Int = 1000
  // let's assume that day starts with all lifts at ground 0
  private val starOfDayStatus = (1 to noOfElevators).map(i => ElevatorStatus(i))
  // should be moved to actor to maintain mutable state, but with Q ok for now
  private val elevatorsState = ListBuffer(starOfDayStatus: _*)

  // queue to receive updates from lifts
  private lazy val queue = Source.queue[ElevatorStatus](100, OverflowStrategy.dropNew)
    .throttle(50, 1.second)
    .mapAsync(1) { e =>
      updateState(e) // tell actor as alternative
    }
    .toMat(Sink.ignore)(Keep.left)
    .run()

  // should not be used without queue with multithreading
  private def updateState(newStatus: ElevatorStatus): Future[Unit] = Future {
    val updateContMeet = newStatus.floor <= noOfFloors && newStatus.goalFloor.map(f => f <= noOfFloors).reduce(_ && _)
    val noStatusChange: ElevatorStatus => Boolean = status => status.id == newStatus.id && status.floor == newStatus.floor && status.goalFloor == newStatus.goalFloor

    if (newStatus.id <= noOfElevators) {
      elevatorsState.find(status => status.id == newStatus.id)
        .foreach {
          case s if noStatusChange(s) =>
            logger.debug("No need to update elevator state {}", s.toString)
          case s if updateContMeet =>
            logger.debug(s"Got state update for elevator: {} moving from floor: {} to goalFloor: {}", newStatus.id, newStatus.floor, newStatus.goalFloor)
            elevatorsState.update(s.id - 1, newStatus)
          case _ =>
            logger.debug("Illegal elevator state values {}", newStatus.toString)
        }
    }
  }

  // gets state updates from moving lifts
  override def update(newStatus: ElevatorStatus): Unit = queue.offer(newStatus).map {
    case QueueOfferResult.Enqueued    => logger.debug(s"received new status, enqueued {}", newStatus)
    case QueueOfferResult.Dropped     => logger.error(s"dropped new status {}", newStatus)
    case QueueOfferResult.Failure(ex) => logger.error(s"Offer failed {}", ex.getMessage)
    case QueueOfferResult.QueueClosed => logger.error("Source Queue closed")
  }

  override def status(): Seq[ElevatorStatus] =
    elevatorsState
      .sortBy(_.floor)(Ordering.Int)
      .map(e => e.copy(goalFloor = e.goalFloor.sorted(Ordering.Int))).toList

  // call lift
  override def pickup(pickupRequest: PickupRequest): Future[ElevatorStatus] = scheduleRide(pickupRequest)

  // schedule ride
  override def ride(newStatus: ElevatorStatus): Future[ElevatorStatus] = scheduleRide(newStatus)

  // particularly bad, but enough for testing
  override def step(): Unit = Thread.sleep(nextFloorTime)
}
