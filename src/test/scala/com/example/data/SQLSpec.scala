package com.example.data

import org.h2.jdbc.JdbcSQLException
import slick.dbio.Effect.Schema
import slick.jdbc.{ GetResult, SQLActionBuilder }
import slick.lifted.ProvenShape

import scala.concurrent.duration._
import scala.concurrent.Await

class SQLSpec extends SpecBase with DragonTestData {

  import dc.driver.api._
  val db = dc.db

  class DragonTable(tag: Tag) extends Table[Dragon](tag, "DRAGONS") {

    def id: Rep[Int]        = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String]   = column[String]("name")
    def firepower: Rep[Int] = column[Int]("firepower")

    def * : ProvenShape[Dragon] = (id.?, name, firepower).mapTo[Dragon]
  }
  val dragonTable: TableQuery[DragonTable] = TableQuery[DragonTable]

  implicit val parser: GetResult[Dragon] = GetResult(r => Dragon(r.<<, r.<<, r.<<))

  val createTable: DBIOAction[Unit, NoStream, Schema] = dragonTable.schema.create
  val dropTable: DBIOAction[Unit, NoStream, Schema]   = dragonTable.schema.drop
  val createDragonActions                             = dragonTable ++= names.map(Dragon(None, _, r.nextInt(100)))
  val prepareTestData: DBIO[Unit]                     = DBIO.seq(createTable, createDragonActions)

  override def beforeAll(): Unit = Await.result(db.run(prepareTestData), 10 seconds)
  override def afterAll(): Unit  = Await.result(db.run(dropTable), 10 seconds)

  describe("working with plain SQL") {

    it("should work with plain SQL") {

      val allDragons: SQLActionBuilder = sql"select * from dragons"

      val a = allDragons.as[Dragon]

      db.run(a) map { _.size should be(16) }
    }

    it("should produce error at runtime") {

      // Column "NOME" not found; SQL statement:
      // select * from dragons where nome = 'Smaug' [42122-191]
      // org.h2.jdbc.JdbcSQLException: Column "NOME" not found; SQL statement:
      val smaug: SQLActionBuilder = sql"select * from dragons where nome = 'Smaug'"

      val a = smaug.as[Dragon]

      recoverToSucceededIf[JdbcSQLException] { db.run(a) }
    }
  }
}
