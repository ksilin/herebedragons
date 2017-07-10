package com.example.http

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.example.data.{Dragon, DragonRepository}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import concurrent.ExecutionContext.Implicits.global

trait DragonsService extends HttpConfig with DragonRepository with FailFastCirceSupport {

  val dragonsRoute: Route = pathPrefix("dragons") {
    pathEndOrSingleSlash {
      parameter("name") { name: String =>
        get {
          complete(getByName(name) map optionToStatus)
        }
      } ~
      get {
        complete(getAll)
      }
    } ~
    path(IntNumber) { id =>
      pathEndOrSingleSlash {
        get {
          complete(getById(id) map optionToStatus)
        }
      }
    }
  }

  def optionToStatus(opt: Option[Dragon]): ToResponseMarshallable = opt match {
    case Some(f) => (OK, f)
    case None    => NotFound
  }

}
