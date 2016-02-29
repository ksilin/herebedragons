package com.example

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.example.data.{DragonTestData, SpecBase}
import com.example.http.routes.DragonsService
import org.scalatest.Matchers
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._

class DragonsServiceSpec extends SpecBase
  with DragonsService
  with Matchers
  with ScalatestRouteTest
  with DragonTestData {

  import dc.driver.api._

  val createDragonActions: DBIO[Option[Int]] = dragonTable ++= dragonsWithId

  override def beforeAll() = {
    val initActions: DBIO[Unit] = DBIO.seq(createTableAction, createDragonActions)
    Await.result(db.run(initActions), 10 seconds)
  }

  describe("Dragons service"){

    it("should retrieve all dragons list") {
        Get("/dragons") ~> dragonsRoute ~> check {
          val responseString: String = responseAs[String]
          println(s"response: $responseString")
          responseAs[JsArray] should be(dragonsWithId.toJson)
        }
      }
    }

    it("should retrieve dragons by id") {
        Get("/dragons/1") ~> dragonsRoute ~> check {
          val responseString: String = responseAs[String]
          println(s"response: $responseString")
          responseAs[JsObject] should be(dragonsWithId.head.toJson)
        }
      }

    it("should retrieve dragons by name") {
        Get("/dragons?name=Smaug") ~> dragonsRoute ~> check {
          val responseString: String = responseAs[String]
          println(s"response: $responseString")
          responseAs[JsObject] should be(dragonsWithId.filter(_.name == "Smaug").head.toJson)
        }
      }
}
