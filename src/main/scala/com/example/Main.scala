package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.example.data.DragonTestData
import com.example.http.routes.DragonsService
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext

object Main extends App with DragonsService with DragonTestData {

  implicit val system                          = ActorSystem("dragons")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContext      = system.dispatcher

  val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("db.inmem_test")

  createTable()
    .flatMap(_ ⇒ createAll(dragonsWithId))
    .flatMap(_ ⇒ Http().bindAndHandle(dragonsRoute, httpInterface, httpPort))

}
