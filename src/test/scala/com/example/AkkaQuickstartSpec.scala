//#full-example
package com.example

import org.scalatest.{ BeforeAndAfterAll, WordSpecLike, Matchers }
import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe }
import scala.concurrent.duration._
import scala.language.postfixOps
import Greeter._
import Goodbyer._
import Printer._

//#test-classes
class AkkaQuickstartSpec(_system: ActorSystem)
  extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {
  //#test-classes

  def this() = this(ActorSystem("AkkaQuickstartSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  //#first-test
  //#specification-example
  "A Greeter Actor" should {
    "pass on a greeting message when instructed to" in {
      //#specification-example
      val testProbe = TestProbe()
      val helloGreetingMessage = "hello"
      val helloGreeter = system.actorOf(Greeter.props(helloGreetingMessage, testProbe.ref))
      val greetPerson = "Akka"
      helloGreeter ! WhoToGreet(greetPerson)
      helloGreeter ! Greet
      testProbe.expectMsg(500 millis, Greeting(helloGreetingMessage + ", " + greetPerson))
    }
  }
  //#first-test

  "A Goodbyer Actor" should {
    "pass on a goodbye message when instructed to" in {
      //#specification-example
      val testProbe = TestProbe()
      val goodbyeMessage = "Auf wiedersehen"
      val goodbyer = system.actorOf(Goodbyer.props(goodbyeMessage, testProbe.ref))
      val goodbyePerson = "Peter"
      goodbyer ! WhoToGoodbye(goodbyePerson)
      goodbyer ! Goodbye
      testProbe.expectMsg(500 millis, GoodbyeMessage(goodbyeMessage + ", " + goodbyePerson))
    }
  }
}
//#full-example
