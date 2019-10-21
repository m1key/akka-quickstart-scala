package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /**
   * Method 1. Manually created router.
   */
  class Master extends Actor {
    // 1 create routees
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)

      ActorRefRoutee(slave)
    }

    // 2 - define router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      // step 3 - route the messages
      case message => router.route(message, sender())
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("Routers", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])

//  for (i <- 1 to 10) {
//    master ! s"Hello $i"
//  }

  /**
   * Method 2. Pool router.
   */
  // 2.1 in code
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  for (i <- 1 to 10) {
    poolMaster ! s"Hello $i"
  }

  // 2.2 from config
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for (i <- 1 to 10) {
    poolMaster ! s"Good morning $i"
  }

  /**
   * Method 3. Router with actors created elsewhere.
   * Group router.
   */
  // in another part of my application
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  // 3.1
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
  for (i <- 1 to 10) {
    groupMaster ! s"Dzien dobry $i"
  }

  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  for (i <- 1 to 10) {
    groupMaster2 ! s"Guten tag $i"
  }

  groupMaster2 ! Broadcast("hello, everyone")

}
