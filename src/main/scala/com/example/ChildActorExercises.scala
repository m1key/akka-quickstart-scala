package com.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.example.ChildActorExercises.WordCounterMaster.{Initialise, WordCountReply, WordCountTask}

object ChildActorExercises extends App {

  // Distributed word counting.
  object WordCounterMaster {
    case class Initialise(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor {
    override def receive: Receive = {
      case Initialise(nChildren) => context.become(withChildren(initialiseChildren(nChildren), 0, 0, Map()))
    }

    def withChildren(children: Array[ActorRef], childToUse: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case string: String =>
        val originalSender = sender()
        println(s"Sending [$string] to child($childToUse)")
        children(childToUse) ! WordCountTask(currentTaskId, string)
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(children, (childToUse + 1) % children.length, currentTaskId + 1, newRequestMap))
      case WordCountReply(id, n) => println(s"Counted $n for $id.")
        requestMap(id) ! n
        context.become(withChildren(children, childToUse, currentTaskId, requestMap - id))
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
      case WordCountTask(id, string) => sender() ! WordCountReply(id, string.split("\\s").length)
    }
  }

  class TestActor extends Actor {
    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialise(3)
        val texts = List("I love Akka", "another message", "yes", "me too", "test test test")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] I received a reply $count")
    }
  }

  val system = ActorSystem("ChildActorExercises")
//  val master = system.actorOf(Props[WordCounterMaster])
//  master ! Initialise(10)
//  master ! "Akka is awesome"
//  master ! "Akka is pretty good"
//  master ! ""
//  master ! "How many are here?\nLet's see."
//  master ! "Akka"
//  master ! "  "
//  master ! "Number seven"
//  master ! "VIII"
//  master ! "Another test."
//  master ! "Scala"
//  master ! "Eleven!"
  val testActor = system.actorOf(Props[TestActor])
  testActor ! "go"

}
