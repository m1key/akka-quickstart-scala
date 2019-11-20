package com.streams

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

/**
 * Back-pressure protocol is transparent to us, but we can control it.
 */
object BackpressureBasics extends App {

  implicit val system = ActorSystem("BackpressureBasics")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] {
    x => // Simulate long processing.
      Thread.sleep(500)
      println(s"Sink $x")
  }

  // We need to add async because otherwise it will just go one by one. No back pressure.
//  fastSource.async.to(slowSink).run()

  val simpleFlow = Flow[Int].map {
    x =>
      println(s"Incoming $x")
      x + 1
  }

  // This is good to illustrate. Also, every component runs on a different actor.
  // Simple flow uses its internal buffer. Its size is 16. After 16 elements received, it will send a back-pressure
  // signal to the source.
  // Then, after the sink processes some elements, the flow allows for more elements to come through.
  fastSource.async.via(simpleFlow).async.to(slowSink).run()

  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)

//  fastSource.async.via(bufferedFlow).async.to(slowSink).run()
//  import scala.concurrent.duration._
//  fastSource.throttle(2, 1 second).runWith(Sink.foreach(println))
}
