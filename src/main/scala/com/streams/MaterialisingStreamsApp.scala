package com.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterialisingStreamsApp extends App {

  implicit val system: ActorSystem = ActorSystem("MaterialisingStreams")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
//  val simpleMaterialisedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)
  val sumFuture = source.runWith(sink)

  sumFuture.onComplete {
    case Success(value) => println(s"The sum of all elements is $value")
    case Failure(exception: Exception) => println(exception)
  }

  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
//  val simpleSink = Sink.foreach[Int](println)
  val simpleSink = Sink.reduce[Int]((a, b) => a + b)

  val graph = simpleSource.viaMat(simpleFlow)(Keep.left).toMat(simpleSink)(Keep.right)
//  graph.run()
  graph.run().onComplete {
    case Success(x) => println(s"Finished with $x") // Will print Done if I just println in the sink
    case Failure(exception) => println(exception)
  }

  // source to sink keeps left
  // source run with keeps right

  // Exercise 1. Last word.
  val someWords = Source(List("This", "is", "akka"))
  someWords.toMat(Sink.last)(Keep.right).run().onComplete {
    case Success(message) => println(message)
  }
  someWords.runWith(Sink.last).onComplete {
    case Success(message) => println(message)
  }

  // Exercise 2. Total word count.
  val sentence1 = "This is akka"
  val sentence2 = "Hi"
  val sentence3 = "This is another sentence"
  val someSentences = Source(List(sentence1, sentence2, sentence3))
  someSentences.map(sentence => sentence.split(" ").length).runWith(Sink.reduce[Int]((a, b) => a + b)).onComplete {
    case Success(message) => println(message)
  }
  someSentences.map(sentence => sentence.split(" ").length).runReduce(_ + _).onComplete {
    case Success(message) => println(message)
  }
  someSentences.map(sentence => sentence.split(" ").length).fold[Int](0)(_ + _).runWith(Sink.last).onComplete {
    case Success(message) => println(message)
  }
}
