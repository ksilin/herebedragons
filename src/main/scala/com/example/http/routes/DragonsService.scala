package com.example.http.routes

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.example.data.DragonRepository
import com.example.utils.HttpConfig
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

trait DragonsService extends HttpConfig with DragonRepository with FailFastCirceSupport {

  val dragonsRoute: Route = pathPrefix("dragons") {
    pathEndOrSingleSlash {
      parameter("name") { name: String =>
        get {
          complete(getByName(name))
        }
      } ~
      get {
        complete(getAll())
      }
    } ~
    path(IntNumber) { id =>
      pathEndOrSingleSlash {
        get {
          onSuccess(getById(id)) {
            case Some(f) => complete(OK, f)
            case None    => complete(NotFound)
          }
        }
      }
    }
  }
}
