package io.elevator.tycoon
import akka.actor.ActorSystem
import akka.stream.Materializer
import io.elevator.tycoon.infrastructure.{ ElevatorControlSystemImpl, ElevatorScheduler }

import scala.concurrent.ExecutionContext

object ECSServer extends App {

  new ECS with ElevatorScheduler with ElevatorControlSystemImpl {
    override protected implicit val system: ActorSystem = ActorSystem("ElevatorTycoon")
    override protected implicit val ec: ExecutionContext = system.dispatcher
    override protected implicit val mat: Materializer = Materializer(system)
  }.startup

}
