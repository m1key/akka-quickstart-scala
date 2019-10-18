package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.language.postfixOps

object TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder for simpleActor")

  import scala.concurrent.duration._
  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "reminder"
  } (system.dispatcher)

  import system.dispatcher
  val routine = system.scheduler.schedule(1 second, 2 seconds) {
    simpleActor ! "heartbeat"
  }

  system.scheduler.scheduleOnce(5 seconds) {
    routine.cancel()
  }

}
