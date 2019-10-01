package com.example

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.example.BasicSpec.{BlackHole, LabTestActor, SimpleActor}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A simple actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"
      echoActor ! message

      expectMsg(message)
    }
  }

  "A black hole actor" should {
    "send back some message" in {
      val blackHole = system.actorOf(Props[BlackHole])
      val message = "hello, test"
      blackHole ! message

      expectNoMessage(1 second)
    }
  }

  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])
    "turn the string into upper case" in {
      labTestActor ! "I like Akka"
      val reply = expectMsgType[String]
      assert(reply == "I LIKE AKKA")
    }
    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }
    "reply with favourite tech" in {
      labTestActor ! "FavouriteTech"
      expectMsgAllOf("Scala", "Akka")
    }
    "reply with cool tech in a different way" in {
      labTestActor ! "FavouriteTech"
      val messages = receiveN(2) // Seq[Any]

      import org.scalatest.Matchers._
//      messages should contain "Scala"
    }
    "reply with cool tech in a fancy way" in {
      labTestActor ! "FavouriteTech"

      expectMsgPF() {
        case "Scala" =>
        case "Akka" =>
      }
    }
  }

}

object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" => if(random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "FavouriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase
    }
  }
}
