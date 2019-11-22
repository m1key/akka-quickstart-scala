package com.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}

import scala.concurrent.duration._

object DynamicStreamHandling extends App {

  implicit val system = ActorSystem("DynamicStreamHandling")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // 1. Kill switch.

  val killSwitchFlow = KillSwitches.single[Int]

  val counter = Source(Stream.from(1)).throttle(1, 1 second).log("counter")
  val sink = Sink.ignore

//  val killSwitch = counter.viaMat(killSwitchFlow)(Keep.right).to(sink).run()
//
//  system.scheduler.scheduleOnce(3 seconds) {
//    killSwitch.shutdown()
//  }

  val anotherCounter = Source(Stream.from(1)).throttle(2, 1 seconds)
  val sharedKillSwitch = KillSwitches.shared("oneButtonToRuleThemAll")

  counter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
  anotherCounter.via(sharedKillSwitch.flow).runWith(Sink.ignore)

  system.scheduler.scheduleOnce(3 seconds) {
    sharedKillSwitch.shutdown()
  }

  val dynamicMerge = MergeHub.source[Int]
  val materialisedSink = dynamicMerge.to(Sink.foreach[Int](println)).run()

  Source(1 to 10).runWith(materialisedSink)
  counter.runWith(materialisedSink)

  val dynamicBroadcast = BroadcastHub.sink[Int]
  Source(1 to 100).runWith(dynamicBroadcast)

  materialisedSink.runWith(Sink.ignore)

}
