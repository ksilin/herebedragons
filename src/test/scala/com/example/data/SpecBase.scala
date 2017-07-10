package com.example.data

import com.example.utils.Timed
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{ AsyncFunSpec, BeforeAndAfterAll, Matchers }
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

class SpecBase extends AsyncFunSpec with Matchers with BeforeAndAfterAll with LazyLogging with Timed {

  val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("db.inmem_test")
}
