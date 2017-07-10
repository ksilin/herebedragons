package com.example.data

import org.scalatest.Succeeded
import slick.dbio.Effect.{ Read, Schema, Write }
import slick.jdbc.JdbcBackend
import slick.lifted.{ BaseJoinQuery, ProvenShape }

import scala.concurrent.Await
import scala.concurrent.duration._

class JoinSpec extends SpecBase with DragonRiderTestData {

  import dc.profile.StreamingProfileAction
  import dc.profile.api._

  val db: JdbcBackend#DatabaseDef = dc.db

  class DragonTable(tag: Tag) extends Table[Dragon](tag, "DRAGONS") {

    def id: Rep[Int]              = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String]         = column[String]("name")
    def firepower: Rep[Int]       = column[Int]("firepower")
    def riderId: Rep[Option[Int]] = column[Option[Int]]("riderId")

    // Error:(24, 73) type mismatch;
//    found   : ((Option[Int], String, Int, Option[Int])) => DragonRiderTestData.this.Dragon
//    required: ((Option[Int], String, Int, Option[Int])) => JoinSpec.this.Dragon
//    def * : ProvenShape[Dragon] = (id.?, name, firepower, riderId).mapTo[Dragon]
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

  val addDragons: DBIOAction[Option[Int], NoStream, Write] = dragonTable ++= dragons
  val addRiders: DBIOAction[Option[Int], NoStream, Write]  = riderTable ++= riders

  val addTestData: DBIOAction[Unit, NoStream, Schema with Write] = DBIO.seq(addDragons, addRiders)

  override def beforeAll(): Unit = Await.result(db.run(createTables andThen addTestData), 10 seconds)
  override def afterAll(): Unit  = Await.result(db.run(dropTables), 10 seconds)

  describe("joins") {

    it("cross join") {
      // select s21."name", s22."name" from "DRAGONS" s21, "RIDERS" s22
      val crossJoin: BaseJoinQuery[DragonTable, RiderTable, Dragon, Rider, Seq, DragonTable, RiderTable] =
        dragonTable.join(riderTable)

      val result: StreamingProfileAction[Seq[(Dragon, Rider)], (Dragon, Rider), Read] = crossJoin.result

      db.run(result) map { allPairs =>
        allPairs foreach println
        Succeeded
      }
    }

    it("cross join with mapping") {
      // select s21."name", s22."name" from "DRAGONS" s21, "RIDERS" s22
      val crossJoin: Query[(Rep[String], Rep[String]), (String, String), Seq] =
        dragonTable.join(riderTable) map { case (dragon, rider) => (dragon.name, rider.name) }

      val result: StreamingProfileAction[Seq[(String, String)], (String, String), Read] = crossJoin.result

      db.run(result) map { namePairs =>
        namePairs foreach println
        Succeeded
      }
    }

    it("cross join with for comp") {
      // select s21."name", s22."name" from "DRAGONS" s21, "RIDERS" s22
      val crossJoin: Query[(Rep[String], Rep[String]), (String, String), Seq] = for {
        d <- dragonTable
        r <- riderTable
      } yield (d.name, r.name)

      db.run(crossJoin.result) map { namePairs =>
        namePairs foreach println
        Succeeded
      }
    }

    it("implicit join") {
      // select s23."name", s24."name" from "DRAGONS" s23, "RIDERS" s24 where s23."riderId" = s24."id"
      val implicitJoin: Query[(Rep[String], Rep[String]), (String, String), Seq] = for {
        d <- dragonTable
        r <- riderTable if d.riderId === r.id
      } yield (d.name, r.name)

      db.run(implicitJoin.result) map { realPairs =>
        realPairs foreach println
        Succeeded
      }
    }

    it("explicit join") {
      // select s11."name", s12."name"
      // from "DRAGONS" s11, "RIDERS" s12
      // where s11."riderId" = s12."id"
      val explicitJoin: Query[(Rep[String], Rep[String]), (String, String), Seq] =
        dragonTable
          .join(riderTable)
          .on { case (dragon, rider) => dragon.riderId === rider.id }
          .map { case (dragon, rider) => (dragon.name, rider.name) }

      db.run(explicitJoin.result) map { realPairs =>
        realPairs foreach println
        Succeeded
      }
    }
  }
}
