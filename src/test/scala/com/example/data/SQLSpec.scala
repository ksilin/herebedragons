package com.example.data

import org.h2.jdbc.JdbcSQLException
import slick.dbio.Effect.Schema
import slick.jdbc.{SQLActionBuilder, GetResult}
import slick.profile.SqlStreamingAction

import scala.concurrent.duration._
import scala.concurrent.Await

class SQLSpec extends SpecBase with DragonTestData {

  import dc.driver.api._
  val db = dc.db

  class DragonTable(tag: Tag) extends Table[Dragon](tag, "DRAGONS") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def firepower = column[Int]("firepower")

    def * = (id.?, name, firepower) <>(Dragon.tupled, Dragon.unapply)
  }
  val dragonTable = TableQuery[DragonTable]

  implicit val parser: GetResult[Dragon] = GetResult(r => Dragon(r.<<, r.<<, r.<<))

  val createTable: DBIOAction[Unit, NoStream, Schema] = dragonTable.schema.create
  val dropTable: DBIOAction[Unit, NoStream, Schema] = dragonTable.schema.drop
  val createDragonActions = dragonTable ++= names.map(Dragon(None, _, r.nextInt(100)))
  val prepareTestData: DBIO[Unit] = DBIO.seq(createTable, createDragonActions)

  override def beforeAll() = Await.result(db.run(prepareTestData), 10 seconds)
  override def afterAll() = Await.result(db.run(dropTable), 10 seconds)

  describe("working with plain SQL") {

    it("should work with plain SQL") {

      val allDragons: SQLActionBuilder = sql"select * from dragons"

      val a: SqlStreamingAction[Seq[Dragon], Dragon, Effect] = allDragons.as[Dragon]

      db.run(a) map { _.size should be(16) }
    }


    it("should produce error at runtime") {

      // Column "NOME" not found; SQL statement:
      // select * from dragons where nome = 'Smaug' [42122-191]
      // org.h2.jdbc.JdbcSQLException: Column "NOME" not found; SQL statement:
      val smaug: SQLActionBuilder = sql"select * from dragons where nome = 'Smaug'"

      val a: SqlStreamingAction[Seq[Dragon], Dragon, Effect] = smaug.as[Dragon]

      recoverToSucceededIf[JdbcSQLException] { db.run(a) }
    }
  }
}
