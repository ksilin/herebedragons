package com.example

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.example.http.HttpService
import org.scalatest._

trait BaseServiceTest extends FunSpec with Matchers with ScalatestRouteTest {

  val testUsers = Seq(
//    UserEntity(Some(1), "Arhelmus", "test"),
//    UserEntity(Some(2), "Arch", "test"),
//    UserEntity(Some(3), "Hierarh", "test")
  )

  val testTokens = Seq(
//    TokenEntity(userId = Some(1)),
//    TokenEntity(userId = Some(2)),
//    TokenEntity(userId = Some(3))
  )

//    reloadSchema()

//  Await.result(db.run(users ++= testUsers), 10.seconds)
//  Await.result(db.run(tokens ++= testTokens), 10.seconds)
}
