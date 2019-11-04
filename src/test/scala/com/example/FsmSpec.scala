package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class FsmSpec extends TestKit(ActorSystem("FsmSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll
with OneInstancePerTest {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import FsmSpec._

  def runTestSuite(props: Props): Unit = {
    "error when not initialised" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError("Machine not initialised"))
    }

    "report a product not available" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialise(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("sandwich")
      expectMsg(VendingError("Product not available"))
    }

    "throw a timeout if no money inserted" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialise(Map("coke" -> 10), Map("coke" -> 1))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 1 dollars"))

      within(1.5 seconds) {
        expectMsg(VendingError("Request timed out"))
      }
    }

    "handle the reception of partial money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialise(Map("coke" -> 10), Map("coke" -> 3))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 2 dollars"))

      within(1.5 seconds) {
        expectMsg(VendingError("Request timed out"))
        expectMsg(GiveBackChange(1))
      }
    }

    "deliver the product if I insert all the money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialise(Map("coke" -> 10), Map("coke" -> 3))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }

    "deliver the product and change if I insert more money than necessary" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialise(Map("coke" -> 10), Map("coke" -> 3))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(4)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
    }
  }

  "A vending machine" should {
    runTestSuite(Props[VendingMachine])
  }

  "A vending machine FSM" should {
    runTestSuite(Props[VendingMachineFSM])
  }
}

object FsmSpec {
  case class Initialise(inventory: Map[String, Int], prices: Map[String, Int])
  case class RequestProduct(product: String)
  case class Instruction(product: String)
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)
  case class VendingError(reason: String)
  case object ReceiveMoneyTimeout
  class VendingMachine extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContext = context.dispatcher

    override def receive: Receive = {
      case Initialise(inventory, prices) => context.become(operational(inventory, prices))
      case _ => sender() ! VendingError("Machine not initialised")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) =>
          sender() ! VendingError("Product not available")
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert $price dollars")
          context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
      }
    }

    def waitForMoney(inventory: Map[String, Int],
                     prices: Map[String, Int],
                     product: String,
                     money: Int,
                     moneyTimeoutSchedule: Cancellable,
                     requester: ActorRef): Receive = {
      case ReceiveMoneyTimeout =>
        requester ! VendingError("Request timed out")
        if (money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))
      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()
        val price = prices(product)
        if (money + amount >= price) {
          requester ! Deliver(product)

          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)

          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)

          context.become(operational(newInventory, prices))
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney dollars")
          context.become(waitForMoney(inventory, prices, product, money + amount, startReceiveMoneyTimeoutSchedule, requester))
        }
    }

    private def startReceiveMoneyTimeoutSchedule = context.system.scheduler.scheduleOnce(1 second) {
      self ! ReceiveMoneyTimeout
    }
  }

  trait VendingState
  case object Idle extends VendingState
  case object Operational extends VendingState
  case object WaitForMoney extends VendingState

  trait VendingData
  case object Uninitialised extends VendingData
  case class Initialised(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
  case class WaitForMoneyData(inventory: Map[String, Int],
                              prices: Map[String, Int],
                              product: String,
                              money: Int,
                              requester: ActorRef) extends VendingData

  class VendingMachineFSM extends FSM[VendingState, VendingData] {
    // No receive handler.

    startWith(Idle, Uninitialised)

    when(Idle) {
      case Event(Initialise(inventory, prices), Uninitialised) =>
        goto(Operational) using Initialised(inventory, prices)
        // Equivalent to context.become(operational(inventory, prices))
      case _ =>
        sender() ! VendingError("Machine not initialised")
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialised(inventory, prices)) =>
        inventory.get(product) match {
          case None | Some(0) =>
            sender() ! VendingError("Product not available")
            stay()
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert $price dollars")
            goto(WaitForMoney) using WaitForMoneyData(inventory, prices, product, 0, sender())
        }
    }

    when(WaitForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitForMoneyData(inventory, prices, product, money, requester)) =>
        requester ! VendingError("Request timed out")
        if (money > 0) requester ! GiveBackChange(money)
        goto(Operational) using Initialised(inventory, prices)
      case Event(ReceiveMoney(amount), WaitForMoneyData(inventory, prices, product, money, requester)) =>
        val price = prices(product)
        if (money + amount >= price) {
          requester ! Deliver(product)

          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)

          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)

          goto(Operational) using Initialised(newInventory, prices)
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney dollars")
          stay() using WaitForMoneyData(inventory, prices, product, money + amount, requester )
        }
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError("CommandNotFound")
        stay()
    }

    onTransition {
      case stateA -> stateB => log.info(s"Transitioning from $stateA to $stateB")
    }

    initialize()
  }
}
