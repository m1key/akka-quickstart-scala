package com.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {

  implicit val system: ActorSystem = ActorSystem("OperatorFusion")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 1)
  val simpleSink = Sink.foreach[Int](println)

  // This runs on the same actor.
  // It means an element goes through the whole stream before a new element is admitted.
  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()
  // operator/component fusion



}
