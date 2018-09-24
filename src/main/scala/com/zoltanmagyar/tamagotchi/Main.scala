package com.zoltanmagyar.tamagotchi

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.gracefulStop
import language.postfixOps
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App {
  val system = ActorSystem("tamagotchi")
  val owner = system.actorOf(Props[OwnerActor], "owner")
  val pet = system.actorOf(Props(classOf[PetActor], owner, PetActorConfig()), "pet")
  import system.dispatcher

  Iterator.continually(StdIn.readLine())
    .takeWhile(_ != "exit")
    .map(_.split(" ").toList)
    .flatMap {
      case "activate" :: List(name) =>
        Some(Rise(name))
      case "feed" :: List("meal") =>
        Some(Eat(Meal))
      case "feed" :: List("snack") =>
        Some(Eat(Snack))
      case "sleep" :: _ =>
        Some(Sleep)
      case _ =>
        println("Unknown command")
        None
    }
    .foreach { command =>
      system.scheduler.scheduleOnce(500 millis) {
        pet ! command
      }
    }

  stopActor(pet)
  stopActor(owner)

  def stopActor(actorRef: ActorRef) = Await.result(gracefulStop(actorRef, 5.seconds), 5.seconds)
}
