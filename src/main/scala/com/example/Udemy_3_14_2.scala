package com.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Udemy_3_14_2 extends App {

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor {
    override def receive: Receive = {
      case Vote(name) => context.become(receiveWithCandidate(name))
      case VoteStatusRequest =>
        println("I haven't voted yet.")
        sender() ! VoteStatusReply(None)
    }
    def receiveWithCandidate(name: String): Receive = {
      case Vote(_) => println("Already voted!")
      case VoteStatusRequest =>
        sender() ! VoteStatusReply(Some(name))
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])

  class VoteAggregator extends Actor {
    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        context.become(receiveWithTotal(0, citizens.size, List.empty))
        citizens.foreach(citizen => citizen ! VoteStatusRequest)
    }

    def receiveWithTotal(totalReceived: Int, totalExpected: Int, allNamesSoFar: List[String]): Receive = {
      case VoteStatusReply(name) =>
        val totalReceivedIncludingThisOne = totalReceived + 1
        val allNamesIncludingThisOne = name.orNull :: allNamesSoFar
        context.become(receiveWithTotal(totalReceivedIncludingThisOne, totalExpected, allNamesIncludingThisOne))

        if (totalReceivedIncludingThisOne == totalExpected) {
          println(allNamesIncludingThisOne.groupBy(identity).mapValues(_.size))
          context.become(receive)
        }
      case AggregateVotes(_) => println("Sorry, I'm still calculating the old one.")
    }
  }

  val system = ActorSystem("Udemy_3_14_2")

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

}
