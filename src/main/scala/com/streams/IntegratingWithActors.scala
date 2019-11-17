package com.streams

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

object IntegratingWithActors extends App {

  implicit val system: ActorSystem = ActorSystem("IntegratingWithActors")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just received a number: $n")
        sender() ! 2 * n
    }
  }

  // Actor as a flow.
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  val numberSource = Source(1 to 10)
  implicit val timeout: Timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

//  numberSource.via(actorBasedFlow).to(Sink.foreach(println)).run()
//  numberSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run()

  // Actor as a source.
  val actorPoweredSource = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
  val materialisedActorRef = actorPoweredSource.to(Sink.foreach[Int](number => println(s"Actor powered flow number $number"))).run()
//  materialisedActorRef ! 10
//  materialisedActorRef ! akka.actor.Status.Success("Complete")

  // Actor as destination/sink.
  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialised")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream complete")
        context.stop(self)
      case StreamFail(ex) =>
        log.warning(s"Stream failed with $ex")
      case message =>
        log.info(s"Message $message has come to its final resting point")
        sender() ! StreamAck
    }
  }

  val destinationActor = system.actorOf(Props[DestinationActor], "destinationActor")

  val actorPoweredSink = Sink.actorRefWithAck[Int](
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable)
  )

  Source(1 to 10).to(actorPoweredSink).run()
}
