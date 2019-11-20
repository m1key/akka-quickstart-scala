package com.streams

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object AdvancedBackpressure extends App {

  implicit val system: ActorSystem = ActorSystem("IntegratingWithExternalService")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  // dropHead - drop oldest.
  val controlledFlow = Flow[Int].map(_  * 2).buffer(10, OverflowStrategy.dropHead)

  case class PagerEvent(description: String, date: Date, nInstances: Int = 1)
  case class Notification(email: String, pagerEvent: PagerEvent)

  val events = List(
    PagerEvent("Service discovery failed", new Date),
    PagerEvent("Illegal elements in data pipeline", new Date),
    PagerEvent("500s spiked", new Date),
    PagerEvent("Service stopped responding", new Date),
    PagerEvent("Service discovery failed", new Date),
    PagerEvent("Illegal elements in data pipeline", new Date),
    PagerEvent("500s spiked", new Date),
    PagerEvent("Service stopped responding", new Date),
    PagerEvent("Service discovery failed", new Date),
    PagerEvent("Illegal elements in data pipeline", new Date),
    PagerEvent("500s spiked", new Date),
    PagerEvent("500s spiked", new Date),
    PagerEvent("500s spiked", new Date),
  )

  val eventSource = Source(events)

  val onCallEngineer = "daniel@rjvm.com"

  def sendEmail(notification: Notification): Unit =
    println(s"Dear ${notification.email}, you have an event: ${notification.pagerEvent}")

//  val notificationSink = Flow[PagerEvent].map(event => Notification(onCallEngineer, event))
//    .to(Sink.foreach[Notification](sendEmail))

  // standard
//  eventSource.to(notificationSink).run()

  def sendEmailSlow(notification: Notification): Unit = {
    Thread.sleep(1000)
    println(s"Dear ${notification.email}, you have an event: ${notification.pagerEvent}")
  }

  val aggregateNotificationFlow = Flow[PagerEvent].conflate((event1, event2) => {
    val nInstances = event1.nInstances + event2.nInstances
    PagerEvent(s"You have have $nInstances that require your attention", new Date, nInstances)
  }).map(resultingEvent => Notification(onCallEngineer, resultingEvent))

//  val notificationSink = Flow[PagerEvent].map(event => Notification(onCallEngineer, event))
//    .to(Sink.foreach[Notification](sendEmailSlow))

  // This is an alternative to back-pressure.
//  eventSource.via(aggregateNotificationFlow).async.to(Sink.foreach[Notification](sendEmailSlow)).run()

  /*
  Slow producer: extrapolate/expand
   */
  import scala.concurrent.duration._
  val slowCounter = Source(Stream.from(1)).throttle(1, 1 second)
  val hungrySink = Sink.foreach[Int](println)

  val extrapolator = Flow[Int].extrapolate(element => Iterator.from(element))
  val repeater = Flow[Int].extrapolate(element => Iterator.continually(element))

  slowCounter.via(repeater).to(hungrySink).run()
}
