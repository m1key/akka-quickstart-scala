package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import org.apache.spark.internal.Logging

object ActorLifecycle extends App {

  object StartChild
  class LifecycleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }

    override def preStart(): Unit = {
      log.info("I am starting")
    }

    override def postStop(): Unit = {
      log.info("I have stopped")
    }
  }

  val system = ActorSystem("LifeCycleDemo")
//  val parent = system.actorOf(Props[LifecycleActor], "parent")
//  parent ! StartChild
//  parent ! PoisonPill

  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor {
    private val child = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }

  class Child extends Actor with Logging {

    override def preStart(): Unit = {
      log.info("Supervised child started")
    }

    override def postStop(): Unit = {
      log.info("Supervised child stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"Supervised actor restarting because of ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = {
      log.info("supervised actor restarted")
    }

    override def receive: Receive = {
      case Fail =>
        log.warn("child will fail now")
        throw new RuntimeException
      case Check =>
        log.info("alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild

}
