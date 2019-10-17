package com.example

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import scala.io.Source
import scala.language.postfixOps

object BackOffSupervisionPattern extends App {

  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = _
    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null) {
          dataSource = Source.fromFile(new File("src/main/resources/testFiles/important_data.txt"))
        }
        log.info("I've just read some important data: " + dataSource.getLines().toList)
    }

    override def preStart(): Unit = {
      log.info("Persistent actor starting")
    }

    override def postStop(): Unit = {
      log.warning("Persistent actor has stop")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info("Persistent actor restarting")
    }
  }

  val system = ActorSystem("BackOffSupervisionPattern")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor])

//  simpleActor ! ReadFile

  import scala.concurrent.duration._
  val simpleSupervisorProps = BackoffSupervisor.props(BackoffOpts.onFailure(
    Props[FileBasedPersistentActor],
    "simpleBackOffActor",
    3 seconds,
    30 seconds,
    0.2
  ))

  val simpleBackOffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
  simpleBackOffSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(BackoffOpts.onStop(
    Props[FileBasedPersistentActor],
    "stopBackOffActor",
    3 seconds,
    30 seconds,
    0.2
  ).withSupervisorStrategy(OneForOneStrategy() {
    case _ => Stop
  }))

  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
  stopSupervisor ! ReadFile

  class EagerFileBasedPersistentActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testFiles/important_data.txt"))
    }
  }

//  val eagerActor = system.actorOf(Props[EagerFileBasedPersistentActor])
  val repeatedSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFileBasedPersistentActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )

  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

}
