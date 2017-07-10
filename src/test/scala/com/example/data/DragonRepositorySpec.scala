package com.example.data

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

class DragonRepositorySpec extends SpecBase with DragonRepository with DragonTestData {

  import dc.profile.api._

  val createDragonActions: DBIO[Option[Int]] = dragonTable ++= dragons

//  override def beforeAll() = {
//    Await.result(createTable(), 10 seconds)
//    Await.result(db.run(createDragonActions), 10 seconds)
//  }

  override def beforeAll(): Unit = {
    val initActions: DBIO[Unit] = DBIO.seq(createTableAction, createDragonActions)
    Await.result(db.run(initActions), 10 seconds)
  }

  describe("dragons") {

    it("should be present on startup") {
      val eventualDragons: Future[List[Dragon]] = getAll
      eventualDragons map { d => d.size should be(names.size)
      }
    }

    it("should be streamable"){Future(succeed)}

    it("should be updateable"){Future(succeed)}

    it("should be deleteable"){Future(succeed)}

  }
}
