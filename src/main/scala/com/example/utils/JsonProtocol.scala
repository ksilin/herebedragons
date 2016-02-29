package com.example.utils

import com.example.data.Dragon
import spray.json.DefaultJsonProtocol

trait JsonProtocol extends DefaultJsonProtocol {
  implicit val dragonFormat = jsonFormat3(Dragon)
}
