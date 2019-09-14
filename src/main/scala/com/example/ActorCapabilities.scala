package com.example


import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "hi" => context.sender() ! "Hello back!"
      case message: String => println(s"[${context.self.path.name}] I have received $message")
      case number: Int => println(s"[simple actor] I have received a number $number")
      case SpecialMessage(contents) => println(s"[simple actor] I have received a special message $contents")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "hi"
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"
  simpleActor ! 13

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")

  case class SendMessageToYourself(content: String)

  simpleActor ! SendMessageToYourself("hey")

  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)



}
