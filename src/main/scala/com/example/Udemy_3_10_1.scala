package com.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Udemy_3_10_1 extends App {

  case class Increment()
  case class Decrement()
  case class Print()

  class CounterActor(private var value: Int) extends Actor {

    override def receive: Receive = {
      case Increment => value += 1
      case Decrement => value -= 1
      case Print => println(value)
    }
  }

  object CounterActor {
    def props(value: Int): Props = Props(new CounterActor(value))
  }

  val system = ActorSystem("Udemy_3_10_1")
  val counterActor = system.actorOf(CounterActor.props(10))

  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Decrement
  counterActor ! Print

}
