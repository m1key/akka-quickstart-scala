package com.streams

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackpressureBasics extends App {

  implicit val system = ActorSystem("BackpressureBasics")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] {
    x => // Simulate long processing.
      Thread.sleep(1000)
      println(s"Sink $x")
  }

  // We need to add async because otherwise it will just go one by one. No back pressure.
//  fastSource.async.to(slowSink).run()

  val simpleFlow = Flow[Int].map {
    x =>
      println(s"Incoming $x")
      x + 1
  }

//  fastSource.async.via(simpleFlow).async.to(slowSink).run()

  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)

//  fastSource.async.via(bufferedFlow).async.to(slowSink).run()
  import scala.concurrent.duration._
  fastSource.throttle(2, 1 second).runWith(Sink.foreach(println))
}
