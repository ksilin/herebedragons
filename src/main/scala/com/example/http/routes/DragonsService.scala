package com.example.http.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.example.data.DragonRepository
import spray.json._

trait DragonsService extends ServiceBase with DragonRepository {

  val dragonsRoute: Route = pathPrefix("dragons") {
    pathEndOrSingleSlash {
      parameter("name"){ name: String =>
        get {
          complete(getByName(name).map(_.toJson))
        }
      } ~
      get {
        complete(getAll().map(_.toJson))
      }
    } ~
      path(IntNumber) { id =>
        pathEndOrSingleSlash {
          get {
            complete(getById(id).map(_.toJson))
          }
        }
      }
  }
}