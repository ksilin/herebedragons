package com.example.data

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import org.scalatest.Succeeded
import slick.basic.DatabasePublisher
import slick.dbio.Effect.{ All, Read, Schema, Write }
import io.circe.generic.auto._
import io.circe.syntax._
import slick.dbio.Effect
import slick.jdbc.JdbcBackend
import slick.lifted.ProvenShape

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class ActionSpec extends SpecBase with DragonRiderTestData {

  import dc.profile.api._
  val db: JdbcBackend#DatabaseDef = dc.db

  class DragonTable(tag: Tag) extends Table[Dragon](tag, "DRAGONS") {

    def id: Rep[Int]              = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String]         = column[String]("name")
    def firepower: Rep[Int]       = column[Int]("firepower")
    def riderId: Rep[Option[Int]] = column[Option[Int]]("riderId")

    // Error:(29, 51) type mismatch;
//    found   : ((Option[Int], String, Int, Option[Int])) => DragonRiderTestData.this.Dragon
//    required: ((Option[Int], String, Int, Option[Int])) => ActionSpec.this.Dragon
//    def * = (id.?, name, firepower, riderId).mapTo[Dragon]
    def * : ProvenShape[Dragon] = (id.?, name, firepower, riderId) <> (Dragon.tupled, Dragon.unapply)
  }
  val dragonTable: TableQuery[DragonTable] = TableQuery[DragonTable]

  class RiderTable(tag: Tag) extends Table[Rider](tag, "RIDERS") {

    def id: Rep[Int]      = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column[String]("name")
    def ability: Rep[Int] = column[Int]("ability")

    def * : ProvenShape[Rider] = (id.?, name, ability).mapTo[Rider]
  }
  val riderTable: TableQuery[RiderTable] = TableQuery[RiderTable]

  val createTables: DBIOAction[Unit, NoStream, Schema] = DBIO.seq(dragonTable.schema.create, riderTable.schema.create)
  val dropTables: DBIOAction[Unit, NoStream, Schema]   = DBIO.seq(dragonTable.schema.drop, riderTable.schema.drop)
  val truncateTables: DBIOAction[Unit, NoStream, Schema] =
    DBIO.seq(dragonTable.schema.truncate, riderTable.schema.truncate)

  val addDragons = dragonTable ++= dragons
  val addRiders  = riderTable ++= riders

  val addTestData: DBIOAction[Unit, NoStream, Schema with Write] = DBIO.seq(addDragons, addRiders)

  override def beforeAll(): Unit = Await.result(db.run(createTables andThen addTestData), 10 seconds)
  override def afterAll(): Unit  = Await.result(db.run(dropTables), 10 seconds)

  describe("actions") {

    it("select action") {
      val fp50Query: Query[DragonTable, Dragon, Seq] = dragonTable.filter(_.firepower > 50)
      val allDragons: DBIO[Seq[Dragon]]              = fp50Query.result
      val capFirepower: DBIO[Int]                    = fp50Query.map(_.firepower).update(50)
      val deleteAllFirepower: DBIO[Int]              = fp50Query.delete
      Future {
        Succeeded
      }
    }

    it("should create dragons") {

      val createDragonAction: DBIO[Int]    = dragonTable += Dragon(None, "Toothless", 25)
      val eventualToothlessId: Future[Int] = db.run(createDragonAction)
      eventualToothlessId map {
        _ should be > 0
      }
    }

    it("should get the query statements") {
      Future {
        val statements: Iterable[String] = addDragons.statements
        val expectedQuery: Vector[String] =
          Vector("""insert into "DRAGONS" ("name","firepower","riderId")  values (?,?,?)""")
        statements should contain theSameElementsAs expectedQuery
      }
    }

    // 20:02:45.409 [ScalaTest-run-running-ActionSpec] DEBUG s.backend.DatabaseComponent.action - #1: [fused] andThen
    //    1: delete [delete from "DRAGONS"]
    //    2: MultiInsertAction [insert into "DRAGONS" ("name","firepower")  values (?,?)]
    it("should combine actions with andThen combinator") {
      val truncateDragons: DBIO[Int]         = dragonTable.delete
      val truncAndReplace: DBIO[Option[Int]] = truncateDragons andThen addDragons
      db.run(truncAndReplace) map { addedRows =>
        addedRows should be(Some(13))
      }
    }

    it("should combine actions with >> / andThen combinator") {
      val truncateDragons: DBIO[Int]                                 = dragonTable.delete
      val truncateAndReplace: DBIOAction[Option[Int], NoStream, All] = truncateDragons >> addDragons
      db.run(truncateAndReplace) map { addedRows =>
        addedRows should be(Some(13))
      }
    }

    it("should combine actions with DBIO.sequence combinator") {
      val anySeq: Seq[DBIO[Any]] =
        Seq(dragonTable.schema.create, addDragons, dragonTable.delete, dragonTable.schema.drop)
      val combinedAny: DBIO[Seq[Any]]              = DBIO.sequence(anySeq)
      val createSeq: Seq[DBIO[Option[Int]]]        = Seq(addDragons, addDragons, addDragons)
      val combinedCreation: DBIO[Seq[Option[Int]]] = DBIO.sequence(createSeq)
      pending
    }

    it("should map actions") {
      val hiccupQuery: Query[RiderTable, Rider, Seq] = riderTable.filter(_.name === "Hiccup")
      val hiccupAction: DBIO[Option[Rider]]          = hiccupQuery.result.headOption

      def assignDragonToRider(dragonName: String, riderId: Option[Int]) =
        dragonTable += Dragon(None, dragonName, 25, riderId)

      val addHiccupAsDrumRider: DBIO[Int] =
        hiccupAction.flatMap { maybeHiccup: Option[Rider] =>
          maybeHiccup
            .map { h: Rider =>
              assignDragonToRider("Drum", h.id)
            }
            .getOrElse(
              DBIO.failed(new Exception("Hiccup not found"))
            )
        }

      db.run(addHiccupAsDrumRider) map { res =>
        println("attached Drum to Hiccup: " + res)
        res should be > 0
      }
    }

    it("should combine actions with for-comprehension adn results with zip") {
      val getBestPairsAction: DBIOAction[Seq[(Dragon, Rider)], NoStream, Read] = for {
        dragons: Seq[Dragon] <- dragonTable.filter(_.firepower > 50).filter(_.riderId.isEmpty).result
        riders: Seq[Rider]   <- riderTable.filter(r => r.ability > 50).result
      } yield dragons zip riders

      db.run(getBestPairsAction) map { pairs =>
        pairs foreach println
        Succeeded
      }
    }

    it("should combine actions with zip") {
      val getBestAction: DBIO[(Seq[Dragon], Seq[Rider])] =
        dragonTable
          .filter(_.firepower > 50)
          .filter(_.riderId.isEmpty)
          .result
          .zip(riderTable.filter(_.ability > 50).result)

      db.run(getBestAction) map { best =>
        println(best)
        Succeeded
      }
    }

    it("should stream dragons") {

      val dragonStream: DatabasePublisher[Dragon] = db.stream(dragonTable.result)

      implicit val system = ActorSystem("streams")
      implicit val mat    = ActorMaterializer()

      val source: Source[Dragon, NotUsed] = Source.fromPublisher(dragonStream)
      val ds: Future[Done] = source.runWith(Sink.foreach { d: Dragon =>
        println(d.asJson)
      })
      ds map (_ => Succeeded)
    }
  }
}
