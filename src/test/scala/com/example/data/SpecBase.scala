package com.example.data

import com.example.utils.{Timed, Logg}
import org.scalatest.{AsyncFunSpec, BeforeAndAfterAll, Matchers}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

class SpecBase extends AsyncFunSpec with Matchers with BeforeAndAfterAll with Logg with Timed {

  val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("db.inmem_test")
}
