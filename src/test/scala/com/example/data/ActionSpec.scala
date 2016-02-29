package com.example.data

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.Succeeded
import slick.backend.DatabasePublisher
import slick.dbio.Effect.{All, Schema, Write}
import slick.profile.SqlAction
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ActionSpec extends SpecBase with DragonRiderTestData {

  import dc.driver.api._
  val db = dc.db

  class DragonTable(tag: Tag) extends Table[Dragon](tag, "DRAGONS") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def firepower = column[Int]("firepower")
    def riderId = column[Option[Int]]("riderId")

    def * = (id.?, name, firepower, riderId) <>(Dragon.tupled, Dragon.unapply)
  }
  val dragonTable = TableQuery[DragonTable]


  class RiderTable(tag: Tag) extends Table[Rider](tag, "RIDERS") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def ability = column[Int]("ability")

    def * = (id.?, name, ability) <>(Rider.tupled, Rider.unapply)
  }
  val riderTable = TableQuery[RiderTable]

  val createTables: DBIOAction[Unit, NoStream, Schema] = DBIO.seq(dragonTable.schema.create, riderTable.schema.create)
  val dropTables: DBIOAction[Unit, NoStream, Schema] = DBIO.seq(dragonTable.schema.drop, riderTable.schema.drop)

  val addDragons: SqlAction[Option[Int], NoStream, Write] = dragonTable ++= dragons
  val addRiders: SqlAction[Option[Int], NoStream, Write] = riderTable ++= riders

  val addTestData: DBIOAction[Unit, NoStream, Schema with Write] = DBIO.seq(addDragons, addRiders)

  override def beforeAll() = Await.result(db.run(createTables andThen addTestData), 10 seconds)
  override def afterAll() = Await.result(db.run(dropTables), 10 seconds)

  describe("actions") {

    it("select action") {
      val fp50Query: Query[DragonTable, Dragon, Seq] = dragonTable.filter(_.firepower > 50)
      val allDragons: DBIO[Seq[Dragon]] = fp50Query.result
      val capFirepower: DBIO[Int] = fp50Query.map(_.firepower).update(50)
      val deleteAllFirepower: DBIO[Int] = fp50Query.delete
      Future {
        Succeeded
      }
    }

    it("should create dragons") {

      val createDragonAction: DBIO[Int] = dragonTable += Dragon(None, "Toothless", 25)
      val eventualToothlessId: Future[Int] = db.run(createDragonAction)
      eventualToothlessId map {
        _ should be > 0
      }
    }

    it("should get the query statements") {
      Future {
        val statements: Iterable[String] = addDragons.statements
        val expectedQuery: Vector[String] = Vector("""insert into "DRAGONS" ("name","firepower","riderId")  values (?,?,?)""")
        statements should contain theSameElementsAs expectedQuery
      }
    }

    // 20:02:45.409 [ScalaTest-run-running-ActionSpec] DEBUG s.backend.DatabaseComponent.action - #1: [fused] andThen
    //    1: delete [delete from "DRAGONS"]
    //    2: MultiInsertAction [insert into "DRAGONS" ("name","firepower")  values (?,?)]
    it("should combine actions with andThen combinator") {
      val truncateDragons: DBIO[Int] = dragonTable.delete
      val truncAndReplace: DBIO[Option[Int]] = truncateDragons andThen addDragons
      db.run(truncAndReplace) map { addedRows => addedRows should be(Some(13)) }
    }

    it("should combine actions with >> combinator") {
      val truncateDragons: DBIO[Int] = dragonTable.delete
      val truncateAndReplace: DBIOAction[Option[Int], NoStream, All] = truncateDragons >> addDragons
      db.run(truncateAndReplace) map { addedRows => addedRows should be(Some(13)) }
    }

    it("should combine actions with DBIO.sequence combinator") {
      val anySeq: Seq[DBIO[Any]] = Seq(dragonTable.schema.create, addDragons, dragonTable.delete, dragonTable.schema.drop)
      val combinedAny: DBIO[Seq[Any]] = DBIO.sequence(anySeq)
      val createSeq: Seq[DBIO[Option[Int]]] = Seq(addDragons, addDragons, addDragons)
      val combinedCreation: DBIO[Seq[Option[Int]]] = DBIO.sequence(createSeq)
      pending
    }

    it("should map actions") {
      val hiccupQuery: Query[RiderTable, Rider, Seq] =
        riderTable.filter(_.name === "Hiccup")
      val hiccupAction: DBIO[Option[Rider]] =
        hiccupQuery.result.headOption

      def assignDrumToRider(riderId: Option[Int]) =
        dragonTable += Dragon(None, "Drum", 25, riderId)

      val addHiccupAsToothlessRider: DBIO[Int] =
        hiccupAction.flatMap { hiccup: Option[Rider] =>
          hiccup.map { h => assignDrumToRider(h.id)
          }.getOrElse(
            DBIO.failed(new Exception("Hiccup not found"))
          )
        }

      db.run(addHiccupAsToothlessRider) map { res =>
        println("attached Drum to hiccup?" + res)
        res should be > 0
      }
    }

    it("should combine actions with for-comprehension") {
      val resAction: DBIO[(Seq[Dragon], Seq[Rider])] = for {
        dragons: Seq[Dragon] <- dragonTable.filter(_.firepower > 50).filter(_.riderId.isEmpty).result
        riders: Seq[Rider] <- riderTable.filter(r => r.ability > 50).result
      } yield (dragons, riders)

      db.run(resAction) map { s =>
        println(s)
        Succeeded
      }
    }

    it("should combine actions with zip") {
      val resAction: DBIO[(Seq[Dragon], Seq[Rider])] =
        dragonTable.filter(_.firepower > 50).filter(_.riderId.isEmpty).result
          .zip(riderTable.filter(_.ability > 50).result)

      db.run(resAction) map { s =>
        println(s)
        Succeeded
      }
    }


    it("should stream dragons") {

      val dragonStream: DatabasePublisher[Dragon] = db.stream(dragonTable.result)

      implicit val system = ActorSystem("streams")
      implicit val mat = ActorMaterializer()

      import DefaultJsonProtocol._
      import spray.json._

      implicit val dragonFormat = jsonFormat4(Dragon)

      val source: Source[Dragon, Unit] = Source.fromPublisher(dragonStream)
      val ds: Future[Unit] = source.runWith(Sink.foreach { d: Dragon => println(d.toJson) })
      ds map (_ => Succeeded)
    }
  }
}
