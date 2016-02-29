package com.example

package object data {
  case class Dragon(id: Option[Int] = None, name: String, firepower: Int)
  case class Rider(id: Option[Int] = None, name: String, ability: Int)
}
