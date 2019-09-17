package com.example

import akka.actor.{Actor, ActorSystem, Props}
import com.example.Udemy_3_14_1.CounterActor.{Decrement, Increment, Print}

object Udemy_3_14_1 extends App {

  class CounterActor() extends Actor {
    import CounterActor._

    override def receive: Receive = {

      case Increment => context.become(receiveIncrement(1))
      case Decrement => context.become(receiveIncrement(-1), false)
      case Print => println(0)
    }

    def receiveIncrement(i: Int) : Receive = {
      case Increment => context.become(receiveIncrement(i+1))
      case Decrement => context.become(receiveIncrement(i-1))
      case Print => println(i)
    }
  }

  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }

  val system = ActorSystem("Udemy_3_14_1")
  val counterActor = system.actorOf(Props[CounterActor])

//  counterActor ! Increment
//  counterActor ! Increment
  counterActor ! Decrement
  counterActor ! Decrement
  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Print


}
