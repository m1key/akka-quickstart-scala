package com.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object GraphicBasics extends App {

  implicit val system: ActorSystem = ActorSystem("GraphicBasics")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)
  val output = Sink.foreach[(Int, Int)](println)

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2)) // fan out
      val zip = builder.add(Zip[Int, Int]) // fan in

      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      ClosedShape
    }
  )

//  graph.run()

  /**
   * Exercise 1. Feed a source into 2 sinks at the same time.
   */
  val sourceOfNumbers = Source(1 to 50)
  val sink1 = Sink.foreach[Int](a => println(s"Sink 1: $a"))
  val sink2 = Sink.foreach[Int](a => println(s"Sink 2: $a"))

//  sourceOfNumbers.to(sink1).run()

  val doubleSinkGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2)) // fan out

//      sourceOfNumbers ~> broadcast

//      broadcast.out(0) ~> sink1
//      broadcast.out(1) ~> sink2

      // or...

      sourceOfNumbers ~> broadcast ~> sink1
                         broadcast ~> sink2

      ClosedShape
    }
  )

//  doubleSinkGraph.run()

  // Should have used throttle instead...
  class TimedIterator(maxInclusive:Int, sleepMillis: Int) extends Iterator[Int] {
    var current = 0

    override def hasNext: Boolean = current < maxInclusive

    override def next(): Int = {
      Thread.sleep(sleepMillis) // This is wrong. It would sleep on the same thread.
      current = current + 1
      current
    }
  }

  /**
   * Exercise 2.
   */

//  val fastSource = Source.fromIterator(() => new TimedIterator(100, 100))
//  val slowSource = Source.fromIterator(() => new TimedIterator(100, 1000))

  import scala.concurrent.duration._
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

//  fastSource.to(sink1).run()

  val countingSink1 = Sink.fold[Int, Int](0)((count, element) => {
    println(s"Sink 1 number of elements $count")
    count + 1
  })
  val countingSink2 = Sink.fold[Int, Int](0)((count, element) => {
    println(s"Sink 2 number of elements $count")
    count + 1
  })
  val mergeAndBalanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge
      slowSource ~> merge

      merge ~> balance

      balance ~> countingSink1
      balance ~> countingSink2

      ClosedShape
    }
  )

  mergeAndBalanceGraph.run()
}
