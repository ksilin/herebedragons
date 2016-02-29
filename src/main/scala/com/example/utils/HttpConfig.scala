package com.example.utils

import com.typesafe.config.ConfigFactory

trait HttpConfig {

  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")

  val httpInterface = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")
}
