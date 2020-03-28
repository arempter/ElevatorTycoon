package io.elevator.tycoon

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.elevator.tycoon.domain.{ ElevatorControlSystem, ElevatorStatus }
import io.elevator.tycoon.infrastructure.ElevatorControlSystemImpl
import org.scalatest.diagrams.Diagrams
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext

class ECSImplSpec extends AnyFlatSpec with Diagrams {

  private val testSystem = ActorSystem("testSystem")
  private val testEC = testSystem.dispatcher
  private val testMat = Materializer(testSystem)

  def withECS(testCode: ElevatorControlSystemImpl => Any) = {
    try {
      testCode(elevatorControlSystemImplMock)
    } catch {
      case e: Throwable => throw e
    }
  }

  private val elevatorControlSystemImplMock = new ElevatorControlSystemImpl {
    override protected implicit val system: ActorSystem = testSystem
    override protected implicit val ec: ExecutionContext = testEC
    override protected implicit val mat: Materializer = testMat
    override val nextFloorTime: Int = 10
  }

  private def findId(ecs: ElevatorControlSystem, id: Int): Option[ElevatorStatus] = ecs.status().find(e => e.id == id).headOption

  "ECSImplSpec" should "not change state for the same values" in withECS { ecs =>
    val ns = ElevatorStatus(1)
    ecs.update(ns)
    Thread.sleep(100)

    findId(ecs, 1).map(lift => assert(lift.id == 1 && lift.floor == 0 && lift.goalFloor == 0))
  }
  it should "update global state on Elevator state change" in withECS { ecs =>
    val ns = ElevatorStatus(15, 2, 3)
    ecs.update(ns)
    Thread.sleep(100)
    findId(ecs, ns.id).map(lift => assert(lift.id == 15 && lift.floor == 2 && lift.goalFloor == 3))
  }

  it should "not update state if floor number is incorrect" in withECS { ecs =>
    val ns = ElevatorStatus(16, 2, 11)
    ecs.update(ns)
    Thread.sleep(100)
    findId(ecs, ns.id).map(lift => assert(lift.id == 16 && lift.floor == 0 && lift.goalFloor == 0))
  }

  it should "not update state if elevator id is incorrect" in withECS { ecs =>
    val ns = ElevatorStatus(20)
    ecs.update(ns)
    Thread.sleep(100)
    assert(ecs.status().size <= 16)
  }

}
