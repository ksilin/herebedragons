package com.example.utils

import org.slf4j.LoggerFactory

trait Logg {
  val log = LoggerFactory.getLogger(getClass)
}