package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.pattern.pipe
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.example.AskSpec.{AuthManager, PipedAuthManager}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "A piped authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }

  private def authenticatorTestSuite(props: Props) = {
    import AskSpec._
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthenticationFailure(AuthManager.AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "rtfm")
      expectMsg(AuthenticationFailure(AuthManager.AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthenticationSuccess)
    }
  }
}

object AskSpec {

  // Assume this code is elsewhere in the code base.
  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value $value for key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // User authenticator actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthenticationFailure(message: String)
  case object AuthenticationSuccess
  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }
  class AuthManager extends Actor with ActorLogging {

    import AuthManager._

    // Step 2 - logistics
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb: ActorRef = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)
    }

    def handleAuthentication(username: String, password: String): Unit = {
      val originalSender = sender()
      // Step 3 ask the actor
      val future = authDb ? Read(username)
      // Step 4 handle the future
      future.onComplete {
        // Step 5 NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ON COMPLETE
        case Success(None) => originalSender ! AuthenticationFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthenticationSuccess
          else originalSender ! AuthenticationFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => originalSender ! AuthenticationFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuthentication(username: String, password: String): Unit = {
      val future = authDb ? Read(username) // Future[Any]
      val passwordFuture = future.mapTo[Option[String]] // Future[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthenticationFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(someDbPassword) =>
          if (someDbPassword == password) AuthenticationSuccess
          else AuthenticationFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      } // Future[Any] - will be completed with the response I send back
      // When the future completes, send the response to the actor ref in the arg list.
      responseFuture.pipeTo(sender())
      // The functionality is identical, but the piped approach is preferred.
    }
  }
}
