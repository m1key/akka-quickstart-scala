package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * Interesting case #1 - custom priority mailbox.
   * P0, P1, P2, P3.
   */

  // Step 1 - mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(
    PriorityGenerator {
      case message: String if message.startsWith("[P0]") => 0
      case message: String if message.startsWith("[P1]") => 1
      case message: String if message.startsWith("[P2]") => 2
      case message: String if message.startsWith("[P3]") => 3
      case _ => 4
    }
  )

  // Step 2 - make it known in the config
  // (See application.conf)

  // Step 3 - attach the dispatcher to an actor
  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
  supportTicketLogger ! PoisonPill
  supportTicketLogger ! "[P3] nice to have"
  supportTicketLogger ! "[P0] must have"
  supportTicketLogger ! "[P1] good to have"

  /**
   * Interesting case #2 - control-aware mailbox
   * We'll use UnboundedControlAwareMailbox
   */
  // Step 1 - mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  // Step 2 - configure who gets the mailbox
  // Make the actor attach to the mailbox

  // Method 1
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
  controlAwareActor ! "[P0] test"
  controlAwareActor ! "[P1] test"
  controlAwareActor ! ManagementTicket

  // Method 2 - using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAwareActor")
  altControlAwareActor ! "[P0] test"
  altControlAwareActor ! "[P1] test"
  altControlAwareActor ! ManagementTicket

}
