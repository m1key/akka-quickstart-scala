package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

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

}
