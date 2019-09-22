package com.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.example.ChildActorExercises.WordCounterMaster.{Initialise, WordCountReply, WordCountTask}

object ChildActorExercises extends App {

  // Distributed word counting.
  object WordCounterMaster {
    private val random = scala.util.Random
    case class Initialise(nChildren: Int)
    case class WordCountTask(text: String)
    case class WordCountReply(count: Int)
  }
  class WordCounterMaster extends Actor {
    override def receive: Receive = {
      case Initialise(nChildren) => context.become(withChildren(initialiseChildren(nChildren), 0))
    }

    def withChildren(children: Array[ActorRef], childToUse: Int): Receive = {
      case string: String =>
        println(s"Sending [$string] to child($childToUse)")
        children(childToUse) ! WordCountTask(string)
        context.become(withChildren(children, (childToUse + 1) % children.length))
      case WordCountReply(n) => println(s"Counted $n.")
        sender() ! n
    }

    def initialiseChildren(nChildren: Int): Array[ActorRef] = {
      println("Initialising children...")
      val children = new Array[ActorRef](nChildren)
      for (i <- 0 until nChildren)
        children(i) = context.actorOf(Props[WordCountWorker], s"child$i")
      children
    }
  }

  class WordCountWorker extends Actor {
    override def receive: Receive = {
      case WordCountTask(string) => sender() ! WordCountReply(string.split("\\s").length)
    }
  }

  val system = ActorSystem("ChildActorExercises")
  val master = system.actorOf(Props[WordCounterMaster])
  master ! Initialise(10)
  master ! "Akka is awesome"
  master ! "Akka is pretty good"
  master ! ""
  master ! "How many are here?\nLet's see."
  master ! "Akka"
  master ! "  "
  master ! "Number seven"
  master ! "VIII"
  master ! "Another test."
  master ! "Scala"
  master ! "Eleven!"

}
