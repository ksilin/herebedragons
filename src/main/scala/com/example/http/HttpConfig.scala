package com.example.http

import com.typesafe.config.ConfigFactory

trait HttpConfig {

  private val config     = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")

  val httpInterface: String = httpConfig.getString("interface")
  val httpPort: Int         = httpConfig.getInt("port")
}
