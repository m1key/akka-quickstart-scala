package com.example

import akka.actor.{Actor, ActorSystem, Props}
import com.example.Udemy_3_10_1.CounterActor.{Decrement, Increment, Print}

object Udemy_3_10_1 extends App {

  class CounterActor() extends Actor {
    import CounterActor._

    var value: Int = 0

    override def receive: Receive = {

      case Increment => value += 1
      case Decrement => value -= 1
      case Print => println(value)
    }
  }

  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }

  val system = ActorSystem("Udemy_3_10_1")
  val counterActor = system.actorOf(Props[CounterActor])

  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Decrement
  counterActor ! Print

}
