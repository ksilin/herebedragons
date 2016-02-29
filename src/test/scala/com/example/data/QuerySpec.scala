package com.example.data

import org.scalatest.Succeeded
import slick.dbio.Effect.Schema
import slick.lifted.CompiledExecutable

import scala.concurrent.Await
import scala.concurrent.duration._

class QuerySpec extends SpecBase with DragonTestData {

  import dc.driver.api._
  val db = dc.db

  class DragonTable(tag: Tag) extends Table[Dragon](tag, "DRAGONS") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.Length(100))
    def firepower = column[Int]("firepower")

    def * = (id.?, name, firepower) <>(Dragon.tupled, Dragon.unapply)
  }

  val dragonTable: TableQuery[DragonTable] = TableQuery[DragonTable]

  val createTable: DBIOAction[Unit, NoStream, Schema] = dragonTable.schema.create
  val dropTable: DBIOAction[Unit, NoStream, Schema] = dragonTable.schema.drop
  val createDragonActions = dragonTable ++= names.map(Dragon(None, _, r.nextInt(100)))
  val prepareTestData: DBIO[Unit] = DBIO.seq(createTable, createDragonActions)

  override def beforeAll() = Await.result(db.run(prepareTestData), 10 seconds)
  override def afterAll() = Await.result(db.run(dropTable), 10 seconds)

  describe("queries") {

    it("should retrieve dragons") {
      // select "id", "name", "firepower" from "DRAGONS"
      db.run(dragonTable.result) map {_.size should be > 10}
    }

    it("should filter"){
      // select "id", "name", "firepower" from "DRAGONS" where "firepower" > 50
      val fp50Query: Query[DragonTable, Dragon, Seq] = dragonTable.filter(_.firepower > 50)
      db.run(fp50Query.result) map { dragons =>
        println(s"dragons: $dragons")
        dragons.size should be < 10
      }
    }

    it("should filter with explicit types"){
      // select "id", "name", "firepower" from "DRAGONS" where "firepower" > 50
      val fp50Query: Query[DragonTable, Dragon, Seq] = dragonTable.filter{ tbl: DragonTable =>
          val fp: Rep[Int] = tbl.firepower
          fp > 50
      }
      db.run(fp50Query.result) map { dragons =>
        println(s"dragons: $dragons")
        dragons.size should be < 10
      }
    }

    it("should perform projections") {
      // select "firepower" from "DRAGONS" order by "firepower"
      val fpQuery: Query[Rep[Int], Int, Seq] = dragonTable.map(_.firepower)
      db.run(fpQuery.result) map { firepower =>
        println(s"firepower: $firepower")
        Succeeded
      }
    }

    it("filters and projections with for comprehension"){
      // select "name" from "DRAGONS" where "firepower" > 50
      val fp50Names: Query[Rep[String], String, Seq] = for {
        dragon <- dragonTable if dragon.firepower > 50
      } yield dragon.name

      db.run(fp50Names.result) map { names =>
        println(s"names: $names")
        Succeeded
      }
    }

    // also available - distinct, count, countDistinct, exists, max, min
    it("should perform aggregate functions 2") {
      // select avg("firepower") from "DRAGONS"
      // Option helps differential between 0 and the absence of a result
      val avgFpQuery: Rep[Option[Int]] = dragonTable.map(_.firepower).avg
      db.run(avgFpQuery.result) map { firepower: Option[Int] =>
        println(s"firepower: $firepower")
        firepower.get should be > 10
      }
    }

    it("should perform sorting") {
      // select "firepower" from "DRAGONS" order by "firepower"
      db.run(dragonTable.map(_.firepower).sorted.result) map {firepower =>
        println(s"firepower: $firepower")
        Succeeded
      }
    }

    // dont be naive here - slick does not know how to shape `dragonTable.groupBy(_.id).result` and will not compile
    it("should perform grouping"){
      db.run(dragonTable.groupBy(_.id).map { case (id, group) => (id, group.map(_.firepower).avg) }.result) map {grouped =>
        println(s"grouped: $grouped")
        grouped.size should be > 10
      }
    }

    it("should limit and offset"){
      // select "id", "name", "firepower" from "DRAGONS" limit 3 offset 10
      val paged: Query[DragonTable, Dragon, Seq] = dragonTable.drop(10).take(3)
      db.run(paged.result) map { dragons =>
        println(s"dragons: $dragons")
        dragons.size should be(3)
      }
    }

    it("should limit and offset - shooting oneself in the foot"){
      // select "id", "name", "firepower" from "DRAGONS" where 1=0
      val paged: Query[DragonTable, Dragon, Seq] = dragonTable.take(3).drop(10)
      db.run(paged.result) map { dragons =>
        println(s"dragons: $dragons")
        dragons.size should be(3)
      }
    }

    it("should limit and offset depends on the order"){
      // select "id", "name", "firepower" from "DRAGONS" limit 3 offset 10
      val paged: Query[DragonTable, Dragon, Seq] = dragonTable.take(10).drop(3)
      db.run(paged.result) map { dragons =>
        println(s"dragons: $dragons")
        dragons.size should be(3)
      }
    }

    it("should offset"){
      // select "id", "name", "firepower" from "DRAGONS" limit -1 offset 10
      db.run(dragonTable.drop(10).result) map {dragons =>
        println(s"dragons: $dragons")
        dragons.size should be < 10
      }
    }

    it("should perform updates - but updates are not queries") {

      val smaug: Query[DragonTable, Dragon, Seq] = dragonTable.filter(_.name === "Smaug")
      val smaugFirepower: Query[Rep[Int], Int, Seq] = smaug.map(_.firepower)

      db.run(smaugFirepower.result) map { newFirePower: Seq[Int] =>
        newFirePower shouldNot contain theSameElementsAs Seq(150)
      }

      db.run(smaugFirepower.update(150)) map { rowsAffected =>
        println(s"rowsAffected: $rowsAffected")
        rowsAffected should be(1)
      }
      db.run(smaugFirepower.result) map { newFirePower: Seq[Int] =>
        newFirePower should contain theSameElementsAs Seq(150)
      }
    }

    it("should perform removal - when transformed into an action") {

      val falkor: Query[DragonTable, Dragon, Seq] = dragonTable.filter(_.name === "Falkor")

      db.run(falkor.delete) map { rowsAffected =>
        println(s"rowsAffected: $rowsAffected")
        rowsAffected should be(1)
      }
      db.run(falkor.result) map { smaugs: Seq[Dragon] =>
        smaugs should be(empty)
      }
    }

    it("should compile queries") {
      val compiledSum: CompiledExecutable[Rep[Option[Int]], Option[Int]] = Compiled(dragonTable.map(_.firepower).sum)
      db.run(compiledSum.result) map { firepower: Option[Int] =>
        println(s"firepower: $firepower")
        firepower.get should be > 10
      }
    }
  }
}
