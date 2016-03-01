package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.example.data.DragonTestData
import com.example.http.routes.DragonsService
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with DragonsService with DragonTestData {

  private implicit val system = ActorSystem("dragons")

  override protected implicit val executor: ExecutionContext = system.dispatcher
  override protected implicit val materializer: ActorMaterializer = ActorMaterializer()

  val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("db.inmem_test")

  createTable()
    .flatMap(_ ⇒ createAll(dragonsWithId))
    .flatMap(_ ⇒ Http().bindAndHandle(dragonsRoute, httpInterface, httpPort))

}
