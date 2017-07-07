package com.example.data

import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.Future

trait DragonRepository {

  val dc: DatabaseConfig[JdbcProfile]
  import dc.driver.api._
  lazy val db: Database = dc.db

  def createAll(dragons: Seq[Dragon]): Future[Option[Int]] = db.run(dragonTable ++= dragons)
  def create(dragon: Dragon): Future[Int]                  = db.run { dragonTable += dragon }

  def createTable(): Future[Unit] = db.run(createTableAction)

  def update(updated: Dragon): Future[Int] =
    updated.id map { id =>
      db.run(byIdQuery(id).update(updated))
    } getOrElse Future.successful(0)

  def getById(id: Int): Future[Option[Dragon]]        = db.run(byIdQuery(id).result.headOption)
  def getByName(name: String): Future[Option[Dragon]] = db.run(byNameQuery(name).result.headOption)

  def getAll: Future[List[Dragon]] = db.run(dragonTable.to[List].result)

  def delete(id: Int): Future[Int] = db.run(byIdQuery(id).delete)

  class DragonTable(tag: Tag) extends Table[Dragon](tag, "DRAGONS") {
    val id: Rep[Int]        = column[Int]("id", O.PrimaryKey, O.AutoInc)
    val name: Rep[String]   = column[String]("name")
    val firepower: Rep[Int] = column[Int]("firepower")

    def * : ProvenShape[Dragon] = (id.?, name, firepower).mapTo[Dragon]
  }

  val dragonTable: TableQuery[DragonTable] = TableQuery[DragonTable]
  lazy val createTableAction: DBIO[Unit]   = dragonTable.schema.create

  def byIdQuery(id: Int): Query[DragonTable, Dragon, Seq]        = dragonTable.filter(_.id === id)
  def byNameQuery(name: String): Query[DragonTable, Dragon, Seq] = dragonTable.filter(_.name === name)
}
