package com.example

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.StatusCodes._
import com.example.data.{DragonTestData, SpecBase}
import com.example.http.DragonsService
import io.circe.Json
import org.scalatest.Matchers
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.Await
import scala.concurrent.duration._

class DragonsServiceSpec
    extends SpecBase
    with DragonsService
    with Matchers
    with ScalatestRouteTest
    with DragonTestData {

  import dc.profile.api._

  val createDragonActions: DBIO[Option[Int]] = dragonTable ++= dragonsWithId

  override def beforeAll(): Unit = {
    val initActions: DBIO[Unit] = DBIO.seq(createTableAction, createDragonActions)
    Await.result(db.run(initActions), 10 seconds)
  }

  describe("Dragons service") {

    it("should retrieve all dragons list") {
      Get("/dragons") ~> dragonsRoute ~> check {
        responseAs[Json] should be(dragonsWithId.asJson)
      }
    }
  }

  it("should retrieve dragons by id") {
    Get("/dragons/1") ~> dragonsRoute ~> check {
      responseAs[Json] should be(dragonsWithId.head.asJson)
    }
  }

  it("should return 404 if dragon id not found") {
    Get("/dragons/999") ~> dragonsRoute ~> check {
      status shouldBe NotFound
    }
  }

  it("should retrieve dragons by name") {
    Get("/dragons?name=Smaug") ~> dragonsRoute ~> check {
      responseAs[Json] should be(dragonsWithId.find(_.name == "Smaug").head.asJson)
    }
  }

  it("should return 404 if dragon name not found") {
    Get("/dragons?name=Oogra") ~> dragonsRoute ~> check {
      status shouldBe NotFound
    }
  }
}
