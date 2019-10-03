package com.example

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class TimedAssertionSpec extends TestKit(
  ActorSystem("TimedAssertionSpec", ConfigFactory.load().getConfig("specialTimedAssertionConfig"))) with ImplicitSender
with WordSpecLike with BeforeAndAfterAll {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertionSpec._

  "a worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])

    "reply in a timely manner" in {
      within(500 millis, 1 second) {
        workerActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a reasonable cadence" in {
      within(1 second) {
        workerActor ! "workSequence"

        val results: Seq[Int] = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages = 10) {
          case WorkResult(result) => result
        }

        assert(results.sum > 5)
      }
    }

    "reply to a test probe in a timely manner" in {
      val probe = TestProbe()
      probe.send(workerActor, "work")
      probe.expectMsg(WorkResult(42)) // timeout of 0.3 seconds (from the config file)
    }
  }
}

object TimedAssertionSpec {

  case class WorkResult(result: Int)

  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSequence" =>
        val r = new Random()
        for (i <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }

}
