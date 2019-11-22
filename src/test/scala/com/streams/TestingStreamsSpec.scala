package com.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

class TestingStreamsSpec extends TestKit(ActorSystem("TestingAkkaStreams"))
with WordSpecLike with BeforeAndAfterAll{

  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A simple stream" should {
    "satisfy basic assertions" in {
      // describe our test
      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)

      val sumFuture = simpleSource.toMat(simpleSink)(Keep.right).run()
      val sum = Await.result(sumFuture, 2 seconds)

      assert(sum == 55)
    }

    "integrate with test actors via materialised values" in {
      import akka.pattern.pipe
      import system.dispatcher

      val simpleSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a: Int, b: Int) => a + b)

      val probe = TestProbe()

      simpleSource.toMat(simpleSink)(Keep.right).run().pipeTo(probe.ref)

      probe.expectMsg(55)
    }

    "integrate with a test actor-based sink" in {
      val simpleSource = Source(1 to 5)
      val flow = Flow[Int].scan[Int](0)(_ + _) // 0, 1, 3, 6, 10, 15
      val streamUnderTest = simpleSource.via(flow)

      val probe = TestProbe()
      val probeSink = Sink.actorRef(probe.ref, "completionMessage")

      streamUnderTest.to(probeSink).run()
      probe.expectMsgAllOf(0, 1, 3, 6, 10, 15)
    }

    "integrate with Streams TestKit Sink" in {
      val sourceUnderTest = Source(1 to 5).map(_ * 2)
      val testSink = TestSink.probe[Int]
      val materialisedTestValue = sourceUnderTest.runWith(testSink)
      materialisedTestValue
        .request(5)
        .expectNext(2, 4, 6 ,8, 10)
        .expectComplete
    }

    "integrate with Streams TestKit Source" in {
      import system.dispatcher
      val sinkUnderTest = Sink.foreach[Int] {
        case 13 => throw new RuntimeException("bad luck")
        case _ =>
      }

      val testSource = TestSource.probe[Int]
      val materialised = testSource.toMat(sinkUnderTest)(Keep.both).run()
      val testPublisher = materialised._1
      val resultFuture = materialised._2

      testPublisher
        .sendNext(1)
        .sendNext(5)
        .sendNext(13)
        .sendComplete()

      resultFuture.onComplete {
        case Success(_) => fail("should have thrown an error")
        case Failure(_) => // OK
      }
    }

    "test flows with a source AND a test sink" in {
      val flowUnderTest = Flow[Int].map(_ * 2)

      val testSource = TestSource.probe[Int]
      val testSink = TestSink.probe[Int]

      val materialised = testSource.via(flowUnderTest).toMat(testSink)(Keep.both).run()
      val (publisher, subscriber) = materialised

      publisher
        .sendNext(1)
        .sendNext(5)
        .sendNext(42)
        .sendNext(99)
        .sendComplete()

      subscriber.request(4)
      subscriber.expectNext(2, 10, 84, 198).expectComplete()
    }
  }
}
