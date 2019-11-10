package com.streams

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

object OpenGraphs extends App {

  implicit val system: ActorSystem = ActorSystem("OpenGraphs")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val concat = builder.add(Concat[Int](2))
      firstSource ~> concat
      secondSource ~> concat
      SourceShape(concat.out)
    }
  )
  sourceGraph.to(Sink.foreach(println)).run()

  val sink1 = Sink.foreach[Int](x => println("sThing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println("sThing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      broadcast ~> sink1
      broadcast ~> sink2
      SinkShape(broadcast.in)
    }
  )

  firstSource.to(sinkGraph).run()

  val flow1 = Flow[Int].map(x => x + 1)
  val flow2 = Flow[Int].map(x => x * 1)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val incrementerShape = builder.add(flow1)
      val multiplierShape = builder.add(flow1)

      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out)
    }
  )

  val exerciseGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sourceShape = builder.add(firstSource)
      val sinkShape = builder.add(sink1)

      FlowShape(sinkShape.in, sourceShape.out)
    }
  )
}