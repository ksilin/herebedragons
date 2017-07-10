package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.example.data.DragonTestData
import com.example.http.DragonsService
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

object Main extends App with DragonsService with DragonTestData {

  implicit val system: ActorSystem             = ActorSystem("dragons")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContext      = system.dispatcher

  override val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("db.inmem_test")

  createTable()
    .flatMap(_ ⇒ createAll(dragonsWithId))
    .flatMap(_ ⇒ Http().bindAndHandle(dragonsRoute, httpInterface, httpPort))

}
