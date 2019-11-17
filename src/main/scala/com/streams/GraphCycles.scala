package com.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, UniformFanInShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, ZipWith}

object GraphCycles extends App {

  implicit val system: ActorSystem = ActorSystem("GraphCycles")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x+1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                   mergeShape <~ incrementerShape

    ClosedShape
  }

  // Deadlock:
//  RunnableGraph.fromGraph(accelerator).run()

  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x+1
    })

    sourceShape ~> mergeShape           ~> incrementerShape
                   mergeShape.preferred <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(actualAccelerator).run()

  val bufferedAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating $x")
      Thread.sleep(100)
      x
    })

    sourceShape ~> mergeShape ~> repeaterShape
    mergeShape <~ repeaterShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(bufferedAccelerator).run()

  val twoSourcesIntoTuple = GraphDSL.create() { implicit builder =>

    val intoTupleShape = builder.add(ZipWith((a: Int, b: Int) => {
      println(s"($a, $b)")
      (a, b)
    }
    ))

    UniformFanInShape(intoTupleShape.out, intoTupleShape.in0, intoTupleShape.in1)
  }

  val source1 = Source(1 to 1)
  val source2 = Source(1 to 1)

  val fibonacciGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val twoSourcesIntoTupleShape = builder.add(twoSourcesIntoTuple)
      val mergeShape = builder.add(MergePreferred[(Int, Int)](1))
      val fibonacciFlowShape = builder.add(Flow[(Int, Int)].map {
        case(a: Int, b:Int) =>
          val fibonacciSum = a + b
          println(s"($b, $a + $b = $fibonacciSum))")
          Thread.sleep(500) // So we can see the results.
          (b, fibonacciSum)
      })

      source1 ~> twoSourcesIntoTupleShape.in(0)
      source2 ~> twoSourcesIntoTupleShape.in(1)

      twoSourcesIntoTupleShape ~> mergeShape           ~> fibonacciFlowShape
                                  mergeShape.preferred <~ fibonacciFlowShape

      ClosedShape
    }
  )

  fibonacciGraph.run()

}
