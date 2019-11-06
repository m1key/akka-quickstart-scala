package com.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object FirstPrinciples extends App {

  implicit val system: ActorSystem = ActorSystem("FirstPrinciples")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  // source
  val source = Source(1 to 10)
  // sink
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
//  graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink)

//  sourceWithFlow.to(sink).run()

  // Exercise 5.1
  val names = Source(List("Michal", "Bob", "John", "Robert", "Robin", "Mark"))
  val namesSink = Sink.foreach[String](println)
  val takeTwo = Flow[String].take(2)

  names.filter(name => name.length >= 5).via(takeTwo).to(namesSink).run()
}
