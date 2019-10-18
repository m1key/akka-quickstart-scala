package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}

import scala.concurrent.duration._

object TimeBased extends App {

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimeBasedSelfClosingActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)
    override def receive: Receive = {
      case Start => log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1000 millis)
      case Reminder => log.info("I am alive")
      case Stop => log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val system = ActorSystem("TimeBased")
  val timerHeartbeatActor = system.actorOf(Props[TimeBasedSelfClosingActor])
  system.scheduler.scheduleOnce(5 seconds) {
    timerHeartbeatActor ! Stop
  }(system.dispatcher)

}
