package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}

import scala.concurrent.duration._

object SelfClosingActor extends App {

  class SCA extends Actor with ActorLogging {
    import system.dispatcher

    override def receive: Receive = open

    def open: Receive = {
      case x =>
        log.info(s"open $x")
        val routine = system.scheduler.scheduleOnce(1 second) {
          log.warning("Closing!")
          context.stop(self)
        }
        context.become(readyToClose(routine))
    }

    def readyToClose(routine: Cancellable): Receive = {
      case x =>
        routine.cancel()
        log.info(s"readyToClose $x")
        context.become(open)
    }

    override def postStop(): Unit = {
      log.info("I have been closed")
    }
  }

  val system = ActorSystem("SelfClosingActor")
  val sca = system.actorOf(Props[SCA])
  sca ! "time to sleep"
  sca ! "or maybe not"

}
