package com.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.scalalogging.StrictLogging

object Udemy_3_10_2 extends App with StrictLogging {

  class BankAccount(private var currentAmount: BigDecimal) extends Actor {
    import BankAccount.{Deposit, Failure, Statement, Success, Withdraw}

    override def receive: Receive = {
      case Deposit(amount) =>
        logger.info(s"Amount [$amount] is being deposited to [${self.path.name}].")
        currentAmount += amount
        logger.info(s"The current amount is [$currentAmount].")
        logger.info(s"Returning success to [${context.sender().path.name}].")
        context.sender() ! Success("Money deposited.")
      case Withdraw(amount) =>
        logger.info(s"Amount [$amount] is being withdrawn from [${self.path.name}].")
        if (currentAmount >= amount) {
          currentAmount -= amount
          logger.info(s"The current amount is [$currentAmount].")
          logger.info(s"Returning success to [${context.sender().path.name}].")
          context.sender() ! Success("Money withdrawn.")
        } else {
          logger.info(s"Cannot withdraw [$amount] as the current amount is only [$currentAmount].")
          logger.info(s"Returning failure to [${context.sender().path.name}].")
          context.sender() ! Failure("You do not have enough gold.")
        }
      case Statement =>
        logger.info("Asking about amount.")
        context.sender() ! currentAmount
      case x =>
        logger.warn(s"Unknown message passed to account: $x")
    }
  }

  object BankAccount {

    case class Deposit(amount: BigDecimal)
    case class Withdraw(amount: BigDecimal)
    case object Statement

    case class Success(message: String = "Success")
    case class Failure(message: String)

    def props(initialAmount: BigDecimal): Props = Props(new BankAccount(initialAmount))
  }

  class BankClient(private val bankAccount: ActorRef) extends Actor {
    import BankAccount.{Deposit, Failure, Statement, Success, Withdraw}

    override def receive: Receive = {
      case deposit: Deposit => bankAccount ! deposit
      case withdraw: Withdraw => bankAccount ! withdraw
      case Statement =>
        bankAccount ! Statement
      case Success(message) => logger.info(s"Success: $message")
      case Failure(message) => logger.info(s"Fail: $message")
      case amount: BigDecimal => logger.info(s"$amount")
    }
  }

  object BankClient {
    def props(bankAccount: ActorRef): Props = Props(new BankClient(bankAccount))
  }

  val system = ActorSystem("Udemy_3_10_2")
  val account = system.actorOf(BankAccount.props(1000), "BankAccount1")
  val client = system.actorOf(BankClient.props(account), "Client1")

  import BankAccount.{Deposit, Statement, Withdraw}
  client ! Statement
  client ! Deposit(90)
  client ! Deposit(9)
  client ! Withdraw(599)
  client ! Withdraw(600)
  client ! Statement

}
