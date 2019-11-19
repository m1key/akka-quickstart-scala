package com.streams

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.Future

object IntegratingWithExternalService extends App {

  implicit val system: ActorSystem = ActorSystem("IntegratingWithExternalService")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()
//  import system.dispatcher // not recommended for mapAsync. Use your own dispatcher.
  implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")

  def genericExtService[A, B](element: A): Future[B] = ???

  // example: simplified PagerDuty

  case class PagerEvent(application: String, description: String, date: Date)

  val eventSource = Source(List(
    PagerEvent("AkkaInfra", "Infrastructure broke", new Date()),
    PagerEvent("abc", "Something broke", new Date()),
    PagerEvent("AkkaInfra", "Service not responding", new Date()),
    PagerEvent("SuperFrontend", "A button does not work", new Date())
  ))

  class PagerActor extends Actor with ActorLogging {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    val emails = Map(
      "Daniel" -> "daniel@rjvm.com",
      "John" -> "john@rjvm.com",
      "Lady Gaga" -> "gaga@rjvm.com"
    )

    def processEvent(pagerEvent: PagerEvent) = {
      val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      println(s"Sending $engineerEmail a notification $pagerEvent")
      Thread.sleep(1000)

      engineerEmail
    }

    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! processEvent(pagerEvent)
    }
  }

  val infraEvents =  eventSource.filter(_.application == "AkkaInfra")
//  val pagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event))
  val pagedEmailSink = Sink.foreach[String](email => println(s"Notified $email"))

//  pagedEngineerEmails.to(pagedEmailSink).run()

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(3 seconds)
  val pagerActor = system.actorOf(Props[PagerActor], "pagerActor")
  val alternativePagerEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String])
  alternativePagerEngineerEmails.to(pagedEmailSink).run()
}
