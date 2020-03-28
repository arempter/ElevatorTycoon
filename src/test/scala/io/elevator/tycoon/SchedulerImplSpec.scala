package io.elevator.tycoon

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.elevator.tycoon.domain.{ ElevatorStatus, PickupRequest }
import io.elevator.tycoon.infrastructure.{ ElevatorControlSystemImpl, ElevatorScheduler }
import org.scalatest.diagrams.Diagrams
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

class SchedulerImplSpec extends AnyFlatSpec with Diagrams {

  implicit val testSystem = ActorSystem("specTest")
  implicit val testEC: ExecutionContext = testSystem.dispatcher

  def withScheduler(testCode: ElevatorScheduler => Any) = {
    try {
      testCode(elevatorSchedulerMock)
    } catch {
      case e: Throwable => throw e
    }
  }

  implicit def elevatorSchedulerMock: ElevatorScheduler = new ElevatorScheduler with ElevatorControlSystemImpl {
    implicit val system: ActorSystem = testSystem
    override protected implicit val ec: ExecutionContext = testEC
    override protected implicit val mat: Materializer = Materializer(system)
    override val nextFloorTime: Int = 100
  }

  private def findId(id: Int, status: Seq[ElevatorStatus]): Option[ElevatorStatus] = status.find(e => e.id == id).headOption

  "SchedulerImplSpec" should "send elevator on user PickupRequest" in withScheduler { scheduler =>
    val rideF = scheduler.scheduleRide(PickupRequest(3, 1))
    Await.ready(rideF, 4.seconds)
    findId(1, scheduler.status()).map(s => assert(s.floor == 3))
  }

  it should "send elevator up on user selecting new floor" in withScheduler { scheduler =>
    val ns = ElevatorStatus(15, 0, 3)
    val rideF = scheduler.scheduleRide(ns)
    Await.ready(rideF, 2.seconds)
    findId(ns.id, scheduler.status()).map(s => assert(s.floor == 3))
  }

  it should "go down on user request" in withScheduler { scheduler =>
    val rideF =
      for {
        r <- scheduler.scheduleRide(PickupRequest(3, 1))
        _ <- scheduler.scheduleRide(ElevatorStatus(r.id, 3, 1))
      } yield r
    Await.ready(rideF, 6.seconds)

    rideF.map(r => findId(r.id, scheduler.status()).map(s => assert(s.floor == 1)))
  }

}
