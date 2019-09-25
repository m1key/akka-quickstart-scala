package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val configString =
    """
      | akka {
      |   loglevel = "DEBUG"
      | }
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("configurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])

  actor ! "Simple message"

  val system2 = ActorSystem("DefaultConfigurationDemo")
  val actor2 = system2.actorOf(Props[SimpleLoggingActor])

  actor2 ! "Simple message"

  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val system3 = ActorSystem("SpecialConfigurationDemo", specialConfig)
  val actor3 = system3.actorOf(Props[SimpleLoggingActor])

  actor3 ! "Simple message"

  val secretConfig = ConfigFactory.load("secret.conf")
  println(secretConfig.getString("akka.loglevel"))
}
